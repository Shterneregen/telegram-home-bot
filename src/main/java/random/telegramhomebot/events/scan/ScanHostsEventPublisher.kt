package random.telegramhomebot.events.scan

import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component
import random.telegramhomebot.utils.logger

@Component
class ScanHostsEventPublisher(private val applicationEventPublisher: ApplicationEventPublisher) {
    private val log = logger()

    fun publishEvent() {
        log.debug("Publishing ScanHostsEvent")
        applicationEventPublisher.publishEvent(ScanHostsEvent(this))
    }
}
