package random.telegramhomebot.integrations.telegram.consume
import org.springframework.stereotype.Service
import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer
import org.telegram.telegrambots.meta.api.objects.Update
import random.telegramhomebot.integrations.telegram.BotApiTransport
import random.telegramhomebot.integrations.telegram.TelegramEvent

@Service
@BotApiTransport
class DefaultTelegramMessageConsumer(private val handler: TelegramMessageHandler) : LongPollingSingleThreadUpdateConsumer {
    override fun consume(update: Update) {
        val callback = update.callbackQuery
        if (callback != null) {
            val message = callback.message ?: return
            val data = callback.data ?: return
            handler.consume(TelegramEvent(callback.from.id, message.chatId, message.messageId.toLong(), data, callback.id))
            return
        }
        val message = update.message ?: return
        if (message.senderChat != null) return
        val actor = message.from ?: return
        val text = message.text ?: return
        handler.consume(TelegramEvent(actor.id, message.chatId, message.messageId.toLong(), text, sentAt = message.date.toLong()))
    }
}
