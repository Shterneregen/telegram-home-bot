package random.telegramhomebot.services.commands

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import random.telegramhomebot.db.model.Host
import random.telegramhomebot.utils.logger
import java.io.ByteArrayOutputStream
import java.util.StringTokenizer
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

@Service
class CommandRunnerService(
    @Value("\${commands.execution.timeout-seconds:30}") private val timeoutSeconds: Long = 30,
    @Value("\${commands.execution.max-output-bytes:65536}") private val maxOutputBytes: Int = 65536,
) {
    private val log = logger()

    init {
        require(timeoutSeconds in 1..300) { "Command timeout must be between 1 and 300 seconds" }
        require(maxOutputBytes in 1024..1048576) { "Command output limit must be between 1024 and 1048576 bytes" }
    }

    // Preserve Runtime.exec(String) tokenization; input does not become shell syntax.
    fun runCommand(command: String): String = execute(StringTokenizer(command).toList().map { it.toString() })

    fun runShellCommand(command: String): String = execute(listOf("/bin/sh", "-c", command))

    internal fun execute(arguments: List<String>): String {
        val descendants = mutableMapOf<Long, ProcessHandle>()
        var process: Process? = null
        var terminate = true
        try {
            val child = ProcessBuilder(arguments).redirectErrorStream(true).start()
            process = child
            child.outputStream.close()
            val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(timeoutSeconds)
            val output = CompletableFuture<String>()
            Thread({
                try {
                    val captured = ByteArrayOutputStream()
                    var truncated = false
                    child.inputStream.use { input ->
                        val buffer = ByteArray(4096)
                        while (true) {
                            val count = input.read(buffer)
                            if (count < 0) break
                            val keep = minOf(count, maxOutputBytes - captured.size())
                            captured.write(buffer, 0, keep)
                            truncated = truncated || keep < count
                        }
                    }
                    output.complete(captured.toString(Charsets.UTF_8) + if (truncated) "\n[output truncated]" else "")
                } catch (e: Exception) {
                    output.completeExceptionally(e)
                }
            }, "command-output").apply { isDaemon = true }.start()
            while (true) {
                child.descendants().use { handles -> handles.forEach { descendants[it.pid()] = it } }
                val remaining = deadline - System.nanoTime()
                if (remaining <= 0) throw TimeoutException()
                if (child.waitFor(minOf(remaining, TimeUnit.MILLISECONDS.toNanos(50)), TimeUnit.NANOSECONDS)) break
            }
            val captured = output.get(maxOf(1, deadline - System.nanoTime()), TimeUnit.NANOSECONDS)
            terminate = false
            return captured
        } catch (_: TimeoutException) {
            log.warn("Command execution deadline exceeded")
            return "Command timed out"
        } catch (_: InterruptedException) {
            Thread.currentThread().interrupt()
            return "Command interrupted"
        } catch (e: Exception) {
            log.warn("Command execution failed ({})", e.javaClass.simpleName)
            return "Command failed"
        } finally {
            process?.takeIf { terminate }?.let { child ->
                child.descendants().use { handles -> handles.forEach { descendants[it.pid()] = it } }
                descendants.values.toList().asReversed().forEach { if (it.isAlive) it.destroyForcibly() }
                if (child.isAlive) child.destroyForcibly()
            }
        }
    }

    fun ping(ip: String) {
        runCommand(String.format("ping -c 4 %s", ip))
        log.debug("ping {}", ip)
    }

    fun pingHosts(hosts: List<Host>) {
        hosts.mapNotNull { it.ip?.takeIf(String::isNotBlank) }.forEach(::ping)
    }
}
