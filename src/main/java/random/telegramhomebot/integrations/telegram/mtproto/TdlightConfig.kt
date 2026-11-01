package random.telegramhomebot.integrations.telegram.mtproto
import dev.telegram.api.TelegramEventHandler
import dev.telegram.tdlight.TdlightBotCredentials
import dev.telegram.tdlight.TdlightClient
import dev.telegram.tdlight.linux.LinuxEgressVerifier
import org.springframework.beans.factory.ObjectProvider
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import random.telegramhomebot.integrations.telegram.BotProperties
import random.telegramhomebot.integrations.telegram.consume.TelegramMessageHandler
import random.telegramhomebot.services.commands.CommandService

@Configuration
@ConditionalOnExpression("\${integrations.telegram.enabled:false} and '\${integrations.telegram.transport:bot-api}' == 'mtproto'")
@EnableConfigurationProperties(MtprotoProperties::class)
class TdlightConfig {
    @Bean fun tdlightEventHandler(handler: ObjectProvider<TelegramMessageHandler>) =
        TelegramEventHandler { event -> handler.getObject().consumeAcknowledged(event) }

    @Bean fun tdlightCommandButtons(commands: CommandService): () -> List<String> =
        { commands.getAllEnabledCommands().map { it.getButtonName() } }

    @Bean fun tdlightTransport(
        settings: MtprotoProperties,
        bot: BotProperties,
        handler: TelegramEventHandler,
        @org.springframework.beans.factory.annotation.Qualifier("tdlightCommandButtons") commandButtons: () -> List<String>,
        @org.springframework.beans.factory.annotation.Value("\${buttons.in.row:3}") buttonsInRow: Int,
    ) = TdlightTransport({
        TdlightClient(
            settings.coreConfig(
                bot.chatId.toLong(),
            ),
            TdlightBotCredentials(bot.token),
            handler,
            LinuxEgressVerifier(),
            onFailure = {
                TdlightFatalShutdown.exit()
            },
        )
    }, commandButtons, buttonsInRow, settings::validate)
}

internal object TdlightFatalShutdown {
    fun exit(
        exitProcess: (Int) -> Unit = { kotlin.system.exitProcess(it) },
        halt: (Int) -> Unit = { Runtime.getRuntime().halt(it) },
        wait: (Long) -> Unit = { Thread.sleep(it) },
    ) {
        // Graceful JVM/Spring shutdown, bounded even if a native shutdown hook hangs.
        Thread({
            Thread({
                wait(55_000)
                halt(1)
            }, "tdlight-exit-watchdog").apply { isDaemon = true }.start()
            exitProcess(1)
        }, "tdlight-fatal-exit").apply { isDaemon = true }.start()
    }
}
