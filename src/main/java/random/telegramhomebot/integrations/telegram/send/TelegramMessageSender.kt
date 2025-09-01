package random.telegramhomebot.integrations.telegram.send

import org.telegram.telegrambots.meta.api.methods.botapimethods.BotApiMethod
import java.io.Serializable

interface TelegramMessageSender {
    fun sendMessage(messageText: String)
    fun <T : Serializable, M : BotApiMethod<T>> execute(method: M): T?
    fun sendMessage(messageText: String, chatId: Long, replyToMessageId: Int? = null)
}
