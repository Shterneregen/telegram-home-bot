package random.telegramhomebot.scheduling

import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Profile
import org.springframework.scheduling.annotation.Async
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import random.telegramhomebot.config.ProfileService
import random.telegramhomebot.services.HostService

@Profile(ProfileService.NETWORK_MONITOR)
@Service
class PingStoredHostsScheduler(private val hostService: HostService) {

    @Async
    @Scheduled(fixedRateString = "\${ping.stored.hosts.scheduled.time}", initialDelay = 10000)
    fun pingStoredHosts() {
        log.debug("PingStoredHostsScheduler :: Ping stored hosts...")
        hostService.pingStoredHosts()
    }

    companion object {
        @Suppress("JAVA_CLASS_ON_COMPANION")
        @JvmStatic
        private val log = LoggerFactory.getLogger(javaClass.enclosingClass)
    }
}