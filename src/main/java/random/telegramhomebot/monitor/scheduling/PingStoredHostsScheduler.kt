package random.telegramhomebot.monitor.scheduling

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.scheduling.annotation.Async
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import random.telegramhomebot.monitor.hosts.HostService
import random.telegramhomebot.utils.logger

@Service
@ConditionalOnProperty(name = ["network-monitor.enabled"], havingValue = "true")
class PingStoredHostsScheduler(private val hostService: HostService) {
    val log = logger()

    @Async
    @Scheduled(fixedRateString = "\${network-monitor.ping-stored-hosts.scheduled-time}", initialDelay = 10000)
    fun pingStoredHosts() {
        log.debug("PingStoredHostsScheduler :: Ping stored hosts...")
        hostService.pingStoredHosts()
    }
}
