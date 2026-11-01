package dev.telegram.api

import java.util.concurrent.CompletableFuture

sealed interface TelegramKeyboard {
    data class Reply(val rows: List<List<String>>) : TelegramKeyboard

    data class Inline(val rows: List<List<TelegramButton>>) : TelegramKeyboard
}

data class OutgoingTelegramMessage(
    val chatId: Long,
    val text: String,
    val replyToMessageId: Long? = null,
    val keyboard: TelegramKeyboard? = null,
)

/** TDLib request completion, not a guarantee of final delivery or reading by the recipient. */
data class TelegramRequestResult(val messageId: Long? = null, val chatId: Long? = null)

/** The operation was never submitted to the native client. */
class TelegramRequestRejectedException : IllegalStateException("Telegram request rejected before submission")

/** A submitted request failed or timed out. Its delivery may still complete; do not blindly resend. */
class TelegramRequestFailedException(val errorCode: Int? = null) : IllegalStateException("Telegram request failed after submission")

sealed interface TelegramDeliveryEvent {
    val chatId: Long
    val oldMessageId: Long

    /** TDLib confirms server-side send success; this is not a read receipt. */
    data class Sent(override val chatId: Long, override val oldMessageId: Long, val messageId: Long) : TelegramDeliveryEvent

    data class Failed(override val chatId: Long, override val oldMessageId: Long, val errorCode: Int?) : TelegramDeliveryEvent
}

/**
 * Futures report request completion, not delivery. Continuations must remain short; use async
 * stages with a host executor for blocking work. Cancellation does not undo native submission.
 * Implementations may retain admission capacity while completion callbacks are running.
 */
interface TelegramMessaging {
    fun send(message: OutgoingTelegramMessage): CompletableFuture<TelegramRequestResult>

    fun edit(
        chatId: Long,
        messageId: Long,
        text: String,
        keyboard: TelegramKeyboard.Inline? = null,
    ): CompletableFuture<TelegramRequestResult>

    fun answerCallback(id: String): CompletableFuture<TelegramRequestResult>
}
