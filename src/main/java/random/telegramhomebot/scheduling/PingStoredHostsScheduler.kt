package random.telegramhomebot.scheduling

import org.springframework.context.annotation.Profile
import org.springframework.scheduling.annotation.Async
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import random.telegramhomebot.config.ProfileService
import random.telegramhomebot.services.hosts.HostService
import random.telegramhomebot.utils.logger

@Profile(ProfileService.NETWORK_MONITOR)
@Service
class PingStoredHostsScheduler(private val hostService: HostService) {
    val log = logger()

    @Async
    @Scheduled(fixedRateString = "\${ping.stored.hosts.scheduled.time}", initialDelay = 10000)
    fun pingStoredHosts() {
        log.debug("PingStoredHostsScheduler :: Ping stored hosts...")
        hostService.pingStoredHosts()
    }
}