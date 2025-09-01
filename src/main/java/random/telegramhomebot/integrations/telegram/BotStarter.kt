package random.telegramhomebot.integrations.telegram

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import org.telegram.telegrambots.longpolling.interfaces.LongPollingUpdateConsumer
import org.telegram.telegrambots.longpolling.starter.SpringLongPollingBot
import random.telegramhomebot.integrations.telegram.consume.DefaultTelegramMessageConsumer


@Component
@ConditionalOnProperty(name = ["integrations.telegram.enabled"], havingValue = "true")
class BotStarter(
    private val botProperties: BotProperties,
    private val consumer: DefaultTelegramMessageConsumer
) : SpringLongPollingBot {
    override fun getBotToken(): String? {
        return botProperties.token
    }

    override fun getUpdatesConsumer(): LongPollingUpdateConsumer? {
        return consumer
    }
}