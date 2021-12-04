package random.telegramhomebot.scheduling

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.scheduling.annotation.Async
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import random.telegramhomebot.services.hosts.HostService
import random.telegramhomebot.utils.logger

@ConditionalOnProperty(prefix = "network-monitor", value = ["enabled"], havingValue = "true")
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
