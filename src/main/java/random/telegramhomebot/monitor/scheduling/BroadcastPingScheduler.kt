package random.telegramhomebot.monitor.scheduling

import jakarta.annotation.PostConstruct
import org.apache.commons.lang3.SystemUtils
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.scheduling.annotation.Async
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import random.telegramhomebot.monitor.network.LanNetworkResolver
import random.telegramhomebot.services.commands.CommandRunnerService
import random.telegramhomebot.utils.logger

@Service
@ConditionalOnProperty(name = ["network-monitor.enabled"], havingValue = "true")
class BroadcastPingScheduler(
    private val commandRunnerService: CommandRunnerService,
    private val lanNetworkResolver: LanNetworkResolver,
) {
    val log = logger()

    @Value("\${network-monitor.broadcast.ping.command.linux}")
    private lateinit var broadcastPingCommandLinux: String

    @Value("\${network-monitor.broadcast.ping.command.windows}")
    private lateinit var broadcastPingCommandWindows: String

    @PostConstruct
    fun validateConfiguration() {
        if (SystemUtils.IS_OS_LINUX && broadcastPingCommandLinux.isBlank()) {
            lanNetworkResolver.resolveCidr()
        }
    }

    @Async
    @Scheduled(fixedRateString = "\${network-monitor.broadcast.ping.scheduled-time}")
    fun broadcastPing() {
        log.debug("Broadcast ping...")
        if (SystemUtils.IS_OS_LINUX && broadcastPingCommandLinux.isNotBlank()) {
            commandRunnerService.runShellCommand(broadcastPingCommandLinux)
            return
        }
        getBroadcastPingCommands().forEach(commandRunnerService::runCommand)
    }

    fun getBroadcastPingCommands(): List<String> {
        return when {
            SystemUtils.IS_OS_WINDOWS -> listOf(broadcastPingCommandWindows).filter(String::isNotBlank)
            SystemUtils.IS_OS_LINUX -> linuxCommands()
            else -> {
                log.warn("Unknown OS. Unable to ping.")
                emptyList()
            }
        }
    }

    private fun linuxCommands(): List<String> =
        broadcastPingCommandLinux
            .takeIf(String::isNotBlank)
            ?.let(::listOf)
            ?: listOf(
                "fping -A -d -a -q -g -i 1 -r 2 ${lanNetworkResolver.resolveCidr()}",
                "arp -a",
            )
}
