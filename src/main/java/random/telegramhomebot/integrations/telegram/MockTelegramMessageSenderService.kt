package random.telegramhomebot.integrations.telegram

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.stereotype.Component
import random.telegramhomebot.utils.logger

@Component
@ConditionalOnMissingBean(DefaultTelegramMessageSenderService::class)
class MockTelegramMessageSenderService : TelegramMessageSenderService {
    val log = logger()
    override fun sendMessage(messageText: String) {
        log.info(messageText)
    }
}
