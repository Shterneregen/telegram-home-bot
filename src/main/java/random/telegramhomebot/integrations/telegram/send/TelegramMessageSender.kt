package random.telegramhomebot.integrations.telegram.send
import dev.telegram.api.TelegramResponse
import java.util.concurrent.CompletableFuture

interface TelegramMessageSender {
    /** Completes on transport response, not final delivery; pre-submit rejection is exceptional. */
    fun sendMessage(messageText: String): CompletableFuture<Void>

    fun sendMessage(
        messageText: String,
        chatId: Long,
        replyToMessageId: Long? = null,
    )

    fun send(
        chatId: Long,
        response: TelegramResponse,
        editMessageId: Long? = null,
    )

    fun answerCallback(id: String)
}
