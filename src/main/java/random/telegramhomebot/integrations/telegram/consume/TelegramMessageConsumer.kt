package random.telegramhomebot.integrations.telegram.consume

import random.telegramhomebot.integrations.telegram.TelegramEvent

interface TelegramMessageConsumer {
    fun consume(event: TelegramEvent)
}
