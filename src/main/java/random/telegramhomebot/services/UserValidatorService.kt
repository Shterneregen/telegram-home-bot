package random.telegramhomebot.services

import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service
import random.telegramhomebot.config.ProfileService
import random.telegramhomebot.telegram.BotProperties
import random.telegramhomebot.utils.logger

@Profile("!${ProfileService.MOCK_BOT}")
@Service
class UserValidatorService(private val botProperties: BotProperties) {
    val log = logger()

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
}