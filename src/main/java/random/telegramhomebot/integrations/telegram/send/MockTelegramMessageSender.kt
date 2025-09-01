package random.telegramhomebot.integrations.telegram.send

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.stereotype.Service
import org.telegram.telegrambots.meta.api.methods.botapimethods.BotApiMethod
import random.telegramhomebot.utils.logger
import java.io.Serializable

@Service
@ConditionalOnMissingBean(DefaultTelegramMessageSender::class)
class MockTelegramMessageSender : TelegramMessageSender {
    val log = logger()
    override fun sendMessage(messageText: String) {
        log.info(messageText)
    }

    override fun sendMessage(messageText: String, chatId: Long, replyToMessageId: Int?) {
        log.info(messageText, chatId, replyToMessageId)
    }

    override fun <T : Serializable, M : BotApiMethod<T>> execute(method: M): T? {
        log.info(method.toString())
        return null
    }
}
