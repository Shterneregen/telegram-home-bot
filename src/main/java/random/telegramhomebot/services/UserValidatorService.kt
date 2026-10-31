package random.telegramhomebot.services

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Service
import random.telegramhomebot.auth.services.LoginAttemptService
import random.telegramhomebot.integrations.telegram.BotProperties
import random.telegramhomebot.utils.logger

@Service
@ConditionalOnProperty(name = ["integrations.telegram.enabled"], havingValue = "true")
class UserValidatorService(
    private val botProperties: BotProperties,
    @Qualifier("botAttemptService")
    private val botAttemptService: LoginAttemptService,
) {
    val log = logger()

    @Synchronized
    fun checkAccess(userId: Long): Boolean {
        if (userId <= 0) return false
        if (botAttemptService.isBlocked(userId.toString())) {
            log.debug("Telegram user is blocked")
            return false
        }

        val allowedUser = isAllowedUser(userId)
        when {
            allowedUser -> botAttemptService.loginSucceeded(userId.toString())
            else -> botAttemptService.loginFailed(userId.toString())
        }
        return allowedUser
    }

    private fun isAllowedUser(userId: Long) = isOwner(userId) || isHomeGroupUser(userId)

    private fun isOwner(userId: Long): Boolean {
        val isOwner = userId == botProperties.chatId.toLong()
        log.debug("Is owner: {}", isOwner)
        return isOwner
    }

    private fun isHomeGroupUser(userId: Long): Boolean {
        val isHomeGroupUser = botProperties.homeGroupUserIds.contains(userId)
        log.debug("Is home group user: {}", isHomeGroupUser)
        return isHomeGroupUser
    }
}
