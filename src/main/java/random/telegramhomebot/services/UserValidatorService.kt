package random.telegramhomebot.services

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Service
import org.telegram.telegrambots.meta.api.objects.Update
import random.telegramhomebot.auth.services.LoginAttemptService
import random.telegramhomebot.telegram.BotProperties
import random.telegramhomebot.utils.logger

@ConditionalOnProperty(prefix = "mock-bot", value = ["enabled"], havingValue = "false")
@Service
class UserValidatorService(
    private val botProperties: BotProperties,
    @Qualifier("botAttemptService")
    private val botAttemptService: LoginAttemptService
) {
    val log = logger()

    fun checkAccessForUpdate(update: Update): Boolean {
        if (!update.hasCallbackQuery() && (!update.hasMessage() || !update.message.hasText())) {
            return false
        }
        val userId = if (update.hasCallbackQuery()) update.callbackQuery.from.id else update.message.from.id
        if (botAttemptService.isBlocked(userId.toString())) {
            log.debug("Bot [$userId] is still blocked")
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
