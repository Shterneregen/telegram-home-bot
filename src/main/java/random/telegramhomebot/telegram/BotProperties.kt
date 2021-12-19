package random.telegramhomebot.telegram

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

@Component
@ConditionalOnProperty(prefix = "mock-bot", value = ["enabled"], havingValue = "false")
@ConfigurationProperties(prefix = "telegram")
class BotProperties {
    lateinit var apiUrl: String
    lateinit var botOwnerId: Number
    lateinit var token: String
    lateinit var botName: String
    lateinit var homeGroupUserIds: List<Long>
}
