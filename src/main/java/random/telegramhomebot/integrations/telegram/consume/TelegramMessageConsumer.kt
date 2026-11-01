package random.telegramhomebot.integrations.telegram.consume

import dev.telegram.api.TelegramEvent

interface TelegramMessageConsumer {
    fun consume(event: TelegramEvent)
}
