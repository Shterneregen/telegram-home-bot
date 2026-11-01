package random.telegramhomebot.integrations.telegram

/** Receives events after transport-level callback acknowledgement; authorization belongs to the application. */
fun interface TelegramEventHandler {
    fun handle(event: TelegramEvent)
}
