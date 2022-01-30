package random.telegramhomebot.services.commands

import org.springframework.stereotype.Service
import random.telegramhomebot.db.model.Host
import random.telegramhomebot.utils.logger
import java.io.BufferedReader
import java.nio.charset.StandardCharsets.UTF_8

@Service
class CommandRunnerService {
    private val log = logger()

    fun runCommand(command: String, encoding: String = UTF_8.name()): String {
        log.debug("Command to run: [{}]", command)
        return try {
            val process: Process = Runtime.getRuntime().exec(command)
            return process.inputStream.bufferedReader().use(BufferedReader::readText)
                .ifBlank { process.errorStream.bufferedReader().use(BufferedReader::readText) }
        } catch (e: Exception) {
            log.error(e.message, e)
            e.message ?: "Some error"
        }
    }

    fun ping(ip: String) {
        if (ip == null || ip.isBlank()) {
            return
        }
        runCommand(String.format("ping -c 4 %s", ip))
        log.debug("ping {}", ip)
    }

    fun pingHosts(hosts: List<Host>) =
        hosts.filter { it.ip != null && it.ip!!.isNotBlank() }.forEach { ping(it.ip!!) }
}
