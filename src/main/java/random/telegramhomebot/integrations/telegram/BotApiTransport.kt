package random.telegramhomebot.integrations.telegram
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@ConditionalOnExpression("\${integrations.telegram.enabled:false} and '\${integrations.telegram.transport:bot-api}' == 'bot-api'")
annotation class BotApiTransport
