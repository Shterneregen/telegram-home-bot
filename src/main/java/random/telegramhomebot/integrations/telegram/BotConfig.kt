package random.telegramhomebot.integrations.telegram

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient

@Configuration
@BotApiTransport
class BotConfig(private val botProperties: BotProperties) {
    @Bean
    fun telegramClient(): OkHttpTelegramClient {
        return OkHttpTelegramClient(botProperties.token)
    }
}
