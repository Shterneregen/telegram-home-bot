package random.telegramhomebot.integrations.telegram

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

@Component
@ConditionalOnProperty(name = ["integrations.telegram.enabled"], havingValue = "true")
@ConfigurationProperties(prefix = "integrations.telegram")
class BotProperties {
    lateinit var chatId: Number
    lateinit var token: String
    lateinit var homeGroupUserIds: List<Long>
}
