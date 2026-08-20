package random.telegramhomebot.services.commands

import org.springframework.stereotype.Service
import random.telegramhomebot.db.model.Host
import random.telegramhomebot.utils.logger
import java.io.BufferedReader

@Service
class CommandRunnerService {
    private val log = logger()

    fun runCommand(command: String): String {
        return execute(command) { Runtime.getRuntime().exec(command) }
    }

    fun runShellCommand(command: String): String {
        return execute(command) { ProcessBuilder("/bin/sh", "-c", command).start() }
    }

    private fun execute(
        command: String,
        processFactory: () -> Process,
    ): String {
        log.debug("Command to run: [{}]", command)
        return try {
            val process = processFactory()
            process.inputStream.bufferedReader().use(BufferedReader::readText)
                .ifBlank { process.errorStream.bufferedReader().use(BufferedReader::readText) }
        } catch (e: Exception) {
            log.error(e.message, e)
            e.message ?: "Some error"
        }
    }

    fun ping(ip: String) {
        runCommand(String.format("ping -c 4 %s", ip))
        log.debug("ping {}", ip)
    }

    fun pingHosts(hosts: List<Host>) {
        hosts.filter { it.ip != null && it.ip!!.isNotBlank() }
            .forEach { ping(it.ip!!) }
    }
}
