package random.telegramhomebot.events.scan

import org.springframework.context.event.EventListener
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import random.telegramhomebot.services.StateChangeService
import random.telegramhomebot.telegram.Bot
import random.telegramhomebot.utils.logger

@Component
class ScanHostsEventListener(
    private val stateChangeService: StateChangeService,
    private val bot: Bot
) {
    private val log = logger()

    @Async
    @EventListener
    fun onEvent(event: ScanHostsEvent) {
        log.debug("Received ScanHostsEvent")
        val checkState = stateChangeService.checkState()
        if (checkState.isNotBlank()) bot.sendMessage(checkState)
    }
}
