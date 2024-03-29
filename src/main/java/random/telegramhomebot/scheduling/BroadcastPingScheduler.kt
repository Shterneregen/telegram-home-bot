package random.telegramhomebot.scheduling

import org.apache.commons.lang3.SystemUtils
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.scheduling.annotation.Async
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import random.telegramhomebot.services.commands.CommandRunnerService
import random.telegramhomebot.utils.logger

@ConditionalOnProperty(prefix = "network-monitor", value = ["enabled"], havingValue = "true")
@Service
class BroadcastPingScheduler(private val commandRunnerService: CommandRunnerService) {
    val log = logger()

    @Value("\${broadcast.ping.command.linux}")
    private lateinit var broadcastPingCommandLinux: String

    @Value("\${broadcast.ping.command.windows}")
    private lateinit var broadcastPingCommandWindows: String

    @Async
    @Scheduled(fixedRateString = "\${broadcast.ping.scheduled.time}")
    fun broadcastPing() {
        log.debug("Broadcast ping...")
        val broadcastPingCommand = getBroadcastPingCommand()
        if (broadcastPingCommand.isNotEmpty()) {
            commandRunnerService.runCommand(broadcastPingCommand)
        }
    }

    fun getBroadcastPingCommand(): String {
        return when {
            SystemUtils.IS_OS_WINDOWS -> broadcastPingCommandWindows
            SystemUtils.IS_OS_LINUX -> broadcastPingCommandLinux
            else -> {
                log.warn("Unknown OS. Unable to ping.")
                ""
            }
        }
    }
}
