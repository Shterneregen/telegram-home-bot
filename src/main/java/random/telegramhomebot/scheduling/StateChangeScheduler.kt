package random.telegramhomebot.scheduling

import org.springframework.context.annotation.Profile
import org.springframework.scheduling.annotation.Async
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import random.telegramhomebot.config.ProfileService
import random.telegramhomebot.events.scan.ScanHostsEventPublisher

@Profile(ProfileService.NETWORK_MONITOR)
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
