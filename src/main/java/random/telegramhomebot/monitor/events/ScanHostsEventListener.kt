package random.telegramhomebot.monitor.events

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.event.EventListener
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import random.telegramhomebot.integrations.telegram.send.TelegramMessageSender
import random.telegramhomebot.monitor.hosts.StateChangeService
import random.telegramhomebot.utils.logger

@Component
@ConditionalOnProperty(name = ["network-monitor.enabled"], havingValue = "true")
class ScanHostsEventListener(
    private val stateChangeService: StateChangeService,
    private val telegramMessageSender: TelegramMessageSender
) {
    private val log = logger()

    @Async
    @EventListener
    fun onEvent(event: ScanHostsEvent) {
        log.debug("Received ScanHostsEvent")
        val checkState = stateChangeService.checkState()
        if (checkState.isNotBlank()) telegramMessageSender.sendMessage(checkState)
    }
}
