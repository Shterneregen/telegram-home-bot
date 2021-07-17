package random.telegramhomebot.telegram

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import random.telegramhomebot.config.ProfileService

@Profile("!${ProfileService.MOCK_BOT}")
@Configuration
@ConfigurationProperties(prefix = "telegram")
class BotProperties {
    lateinit var apiUrl: String
    lateinit var botOwnerId: Number
    lateinit var token: String
    lateinit var botName: String
    lateinit var homeGroupUserIds: List<Long>
}