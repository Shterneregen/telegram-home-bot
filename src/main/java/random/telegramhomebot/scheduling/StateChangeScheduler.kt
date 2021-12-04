package random.telegramhomebot.scheduling

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.scheduling.annotation.Async
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import random.telegramhomebot.events.scan.ScanHostsEventPublisher

@ConditionalOnProperty(prefix = "network-monitor", value = ["enabled"], havingValue = "true")
@Service
class StateChangeScheduler(
    private val scanHostsEventPublisher: ScanHostsEventPublisher
) {

    @Async
    @Scheduled(fixedRateString = "\${state.change.scheduled.time}", initialDelay = 20000)
    fun checkState() {
        scanHostsEventPublisher.publishEvent()
    }
}
