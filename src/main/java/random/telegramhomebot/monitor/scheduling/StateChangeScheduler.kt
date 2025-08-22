package random.telegramhomebot.monitor.scheduling

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.scheduling.annotation.Async
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import random.telegramhomebot.monitor.events.ScanHostsEventPublisher

@Service
@ConditionalOnProperty(name = ["network-monitor.enabled"], havingValue = "true")
class StateChangeScheduler(
    private val scanHostsEventPublisher: ScanHostsEventPublisher
) {

    @Async
    @Scheduled(fixedRateString = "\${network-monitor.state-change.scheduled.time}", initialDelay = 20000)
    fun checkState() {
        scanHostsEventPublisher.publishEvent()
    }
}
