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
    var homeGroupUserIds: List<Long> = emptyList()
    var transport: String = "bot-api"

    @jakarta.annotation.PostConstruct
    fun validate() {
        require(transport in setOf("bot-api", "mtproto")) { "TELEGRAM_TRANSPORT must be bot-api or mtproto" }
        require(token.isNotBlank()) { "TELEGRAM_TOKEN must be set" }
        require(chatId.toLong() != 0L) { "TELEGRAM_BOT_CHAT_ID must be set" }
    }
}
