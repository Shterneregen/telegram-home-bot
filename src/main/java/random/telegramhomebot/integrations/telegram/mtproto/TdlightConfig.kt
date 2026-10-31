package random.telegramhomebot.integrations.telegram.mtproto
import org.springframework.beans.factory.ObjectProvider
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import random.telegramhomebot.integrations.telegram.BotProperties
import random.telegramhomebot.integrations.telegram.consume.TelegramMessageHandler

@Configuration
@ConditionalOnExpression("\${integrations.telegram.enabled:false} and '\${integrations.telegram.transport:bot-api}' == 'mtproto'")
@EnableConfigurationProperties(MtprotoProperties::class)
class TdlightConfig {
    @Bean fun tdlightTransport(
        settings: MtprotoProperties,
        bot: BotProperties,
        handler: ObjectProvider<TelegramMessageHandler>,
        commands: random.telegramhomebot.services.commands.CommandService,
        @org.springframework.beans.factory.annotation.Value("\${buttons.in.row:3}") buttonsInRow: Int,
    ) = TdlightTransport(settings, bot, handler, commands, buttonsInRow, fatalShutdown = {
        // Graceful JVM/Spring shutdown, bounded even if a native shutdown hook hangs.
        Thread({
            Thread({
                Thread.sleep(55_000)
                Runtime.getRuntime().halt(1)
            }, "tdlight-exit-watchdog").apply { isDaemon = true }.start()
            kotlin.system.exitProcess(1)
        }, "tdlight-fatal-exit").apply { isDaemon = true }.start()
    })
}
