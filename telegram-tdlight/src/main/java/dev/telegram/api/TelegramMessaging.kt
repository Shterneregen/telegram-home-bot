package dev.telegram.api

import java.util.concurrent.CompletableFuture

/** Keyboard layout; rows retain their supplied order. */
sealed interface TelegramKeyboard {
    /** Text buttons that send their label as a new user message. */
    data class Reply(val rows: List<List<String>>) : TelegramKeyboard

    /** Buttons attached to a message that return callback data instead of a text message. */
    data class Inline(val rows: List<List<TelegramButton>>) : TelegramKeyboard
}

/**
 * Plain text addressed to an explicit chat, optionally replying to a message in that chat.
 * Message identifiers must come from the same transport/session API; do not substitute Bot API IDs.
 * A null keyboard does not request removal of an existing reply keyboard.
 */
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

/** Asynchronous delivery update, independent of the request future and possibly arriving before it. */
sealed interface TelegramDeliveryEvent {
    /** Destination chat of the original send. */
    val chatId: Long

    /** Temporary message identifier used to correlate the original request with this update. */
    val oldMessageId: Long

    /** TDLib confirms server-side send success; this is not a read receipt. */
    data class Sent(override val chatId: Long, override val oldMessageId: Long, val messageId: Long) : TelegramDeliveryEvent

    /** Delivery failed; only the numeric error code is exposed, never native error text. */
    data class Failed(override val chatId: Long, override val oldMessageId: Long, val errorCode: Int?) : TelegramDeliveryEvent
}

/**
 * Futures report request completion, not delivery. Continuations must remain short; use async
 * stages with a host executor for blocking work. Cancellation does not undo native submission.
 * Implementations may retain admission capacity while completion callbacks are running.
 */
interface TelegramMessaging {
    /** Enqueues a send; rejection and submitted-request failure are reported through the future. */
    fun send(message: OutgoingTelegramMessage): CompletableFuture<TelegramRequestResult>

    /** Replaces message text and inline markup; a null keyboard removes the inline keyboard. */
    fun edit(
        chatId: Long,
        messageId: Long,
        text: String,
        keyboard: TelegramKeyboard.Inline? = null,
    ): CompletableFuture<TelegramRequestResult>

    /** Acknowledges receipt without a popup. Use the opaque callback ID from [TelegramEvent]. */
    fun answerCallback(id: String): CompletableFuture<TelegramRequestResult>
}
