package random.telegramhomebot.integrations.telegram

interface TelegramMessageSenderService {
    fun sendMessage(messageText: String)
}
