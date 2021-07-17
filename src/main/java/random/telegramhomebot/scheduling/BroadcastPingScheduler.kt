package random.telegramhomebot.scheduling

import org.apache.commons.lang3.SystemUtils
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Profile
import org.springframework.scheduling.annotation.Async
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import random.telegramhomebot.config.ProfileService
import random.telegramhomebot.services.CommandRunnerService

@Profile(ProfileService.NETWORK_MONITOR)
@Service
class BroadcastPingScheduler(private val commandRunnerService: CommandRunnerService) {

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

    companion object {
        @Suppress("JAVA_CLASS_ON_COMPANION")
        @JvmStatic
        private val log = LoggerFactory.getLogger(javaClass.enclosingClass)
    }
}