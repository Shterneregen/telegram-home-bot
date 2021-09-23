package random.telegramhomebot.scheduling

import org.springframework.context.annotation.Profile
import org.springframework.scheduling.annotation.Async
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import random.telegramhomebot.config.ProfileService
import random.telegramhomebot.services.StateChangeService
import random.telegramhomebot.telegram.Bot

@Profile(ProfileService.NETWORK_MONITOR)
@Service
class StateChangeScheduler(
    private val stateChangeService: StateChangeService,
    private val bot: Bot
) {

    @Async
    @Scheduled(fixedRateString = "\${state.change.scheduled.time}", initialDelay = 20000)
    fun checkState() {
        val checkState = stateChangeService.checkState()
        if (checkState.isNotBlank()) {
            bot.sendMessage(checkState)
        }
    }
}
