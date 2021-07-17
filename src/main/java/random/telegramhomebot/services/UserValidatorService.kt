package random.telegramhomebot.services

import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service
import random.telegramhomebot.config.ProfileService
import random.telegramhomebot.telegram.BotProperties

@Profile("!${ProfileService.MOCK_BOT}")
@Service
class UserValidatorService(private val botProperties: BotProperties) {

    fun isAllowedUser(userId: Long) = isOwner(userId) || isHomeGroupUser(userId)

    private fun isOwner(userId: Long): Boolean {
        val isOwner = userId == botProperties.botOwnerId.toInt().toLong()
        log.debug("Is owner: {}", isOwner)
        return isOwner
    }

    private fun isHomeGroupUser(userId: Long): Boolean {
        val isHomeGroupUser = botProperties.homeGroupUserIds.contains(userId)
        log.debug("Is home group user: {}", isHomeGroupUser)
        return isHomeGroupUser
    }

    companion object {
        @Suppress("JAVA_CLASS_ON_COMPANION")
        @JvmStatic
        private val log = LoggerFactory.getLogger(javaClass.enclosingClass)
    }
}