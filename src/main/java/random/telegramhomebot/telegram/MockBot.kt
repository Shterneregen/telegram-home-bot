package random.telegramhomebot.telegram

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import random.telegramhomebot.utils.logger

@ConditionalOnProperty(prefix = "mock-bot", value = ["enabled"], havingValue = "true")
@Component
class MockBot : Bot {
    val log = logger()
    override fun sendMessage(messageText: String) {
        log.info(messageText)
    }
}
