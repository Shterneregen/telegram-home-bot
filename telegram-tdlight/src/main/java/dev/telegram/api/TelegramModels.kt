package dev.telegram.api

/** Inline button with a visible label and callback data of 1 to 64 UTF-8 bytes. Keep data free of secrets. */
data class TelegramButton(val text: String, val data: String)

/**
 * Incoming text or callback, without SDK objects or application authorization decisions.
 *
 * @property userId Immediate sender used by the host for authorization, not the destination chat.
 * @property chatId Source chat to which replies should be addressed.
 * @property messageId Transport-native message ID; for callbacks, the message containing the button.
 * @property text Message text, or UTF-8 callback data when [callbackId] is present.
 * @property callbackId Opaque callback identifier; null for ordinary text messages.
 * @property sentAt Unix epoch seconds: message creation time for text, local receipt time for callbacks.
 */
data class TelegramEvent(
    val userId: Long,
    val chatId: Long,
    val messageId: Long,
    val text: String,
    val callbackId: String? = null,
    val sentAt: Long = java.time.Instant.now().epochSecond,
)

/** Plain-text response with optional inline button rows, used for sending or editing a message. */
data class TelegramResponse(val text: String, val buttons: List<List<TelegramButton>>? = null)
