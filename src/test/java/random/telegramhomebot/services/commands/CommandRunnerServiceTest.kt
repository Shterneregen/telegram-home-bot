package random.telegramhomebot.services.commands

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

class CommandRunnerServiceTest {
    @TempDir lateinit var directory: Path

    private fun child(
        mode: String,
        vararg arguments: String,
    ): List<String> =
        listOf(
            Path.of(System.getProperty("java.home"), "bin", "java").toString(),
            "-cp", Path.of(CommandProcessFixture::class.java.protectionDomain.codeSource.location.toURI()).toString(),
            CommandProcessFixture::class.java.name, mode,
        ) + arguments

    @Test fun `large stderr cannot deadlock and captured output is bounded`() {
        val output = CommandRunnerService(10, 1024).execute(child("stderr"))
        assertTrue(output.startsWith("stderr"))
        assertTrue(output.endsWith("[output truncated]"))
        assertTrue(output.length <= 1050)
    }

    @Test fun `timeout kills command and descendants and next command succeeds`() {
        val pids = directory.resolve("pids.txt")
        val runner = CommandRunnerService(3, 1024)
        assertEquals("Command timed out", runner.execute(child("descendants", pids.toString())))
        val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5)
        val handles = Files.readAllLines(pids).map { ProcessHandle.of(it.toLong()).orElse(null) }
        while (handles.any { it?.isAlive == true } && System.nanoTime() < deadline) Thread.sleep(10)
        assertTrue(handles.all { it?.isAlive != true }, "Child processes must be terminated")
        assertEquals("ok", runner.execute(child("ok")))
    }

    @Test fun `interrupt terminates command and preserves interrupt flag`() {
        val pids = directory.resolve("interrupted.txt")
        val result = CompletableFuture<Pair<String, Boolean>>()
        val worker =
            Thread {
                val output = CommandRunnerService(30, 1024).execute(child("sleep", pids.toString()))
                result.complete(output to Thread.currentThread().isInterrupted)
            }
        worker.start()
        try {
            val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5)
            while (!Files.exists(pids) && System.nanoTime() < deadline) Thread.sleep(10)
            assertTrue(Files.exists(pids))
        } finally {
            worker.interrupt()
        }
        assertEquals("Command interrupted" to true, result.get(5, TimeUnit.SECONDS))
    }

    @Test fun `successful command may leave an intentional background process`() {
        val pids = directory.resolve("background.txt")
        val output = CommandRunnerService(10, 1024).execute(child("background", pids.toString()))
        val descendant = ProcessHandle.of(Files.readAllLines(pids)[1].toLong()).orElse(null)
        try {
            assertEquals("started", output)
            assertTrue(descendant?.isAlive == true)
        } finally {
            descendant?.destroyForcibly()
            descendant?.onExit()?.get(5, TimeUnit.SECONDS)
        }
    }
}
