package random.telegramhomebot.telegram

import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import random.telegramhomebot.config.ProfileService

@Profile(ProfileService.MOCK_BOT)
@Component
class MockBot : Bot {
    override fun sendMessage(messageText: String) {
        log.info(messageText)
    }

    companion object {
        @Suppress("JAVA_CLASS_ON_COMPANION")
        @JvmStatic
        private val log = LoggerFactory.getLogger(javaClass.enclosingClass)
    }
}