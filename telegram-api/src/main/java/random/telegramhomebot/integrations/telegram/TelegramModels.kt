package random.telegramhomebot.integrations.telegram

data class TelegramButton(val text: String, val data: String)

data class TelegramEvent(
    val userId: Long,
    val chatId: Long,
    val messageId: Long,
    val text: String,
    val callbackId: String? = null,
    val sentAt: Long = java.time.Instant.now().epochSecond,
)

data class TelegramResponse(val text: String, val buttons: List<List<TelegramButton>>? = null)
