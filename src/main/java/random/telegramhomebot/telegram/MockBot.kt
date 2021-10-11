package random.telegramhomebot.telegram

import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import random.telegramhomebot.config.ProfileService
import random.telegramhomebot.utils.logger

@Profile(ProfileService.MOCK_BOT)
@Component
class MockBot : Bot {
    val log = logger()
    override fun sendMessage(messageText: String) {
        log.info(messageText)
    }
}
