package random.telegramhomebot.integrations.telegram

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.support.DefaultLifecycleProcessor
import random.telegramhomebot.auth.services.LoginAttemptService
import random.telegramhomebot.integrations.telegram.consume.DefaultTelegramMessageConsumer
import random.telegramhomebot.integrations.telegram.consume.TelegramMessageHandler
import random.telegramhomebot.integrations.telegram.menu.CallbackMenuService
import random.telegramhomebot.integrations.telegram.mtproto.TdlightConfig
import random.telegramhomebot.integrations.telegram.mtproto.TdlightTransport
import random.telegramhomebot.integrations.telegram.send.DefaultTelegramMessageSender
import random.telegramhomebot.integrations.telegram.send.MockTelegramMessageSender
import random.telegramhomebot.integrations.telegram.send.TelegramMessageSender
import random.telegramhomebot.services.UserValidatorService
import random.telegramhomebot.services.commands.CommandService

class TransportSelectionTest {
    @Configuration(proxyBeanMethods = false)
    class Dependencies {
        @Bean fun properties() =
            BotProperties().apply {
                token = "123:test"
                chatId = 42L
            }

        @Bean fun commands() = mock(CommandService::class.java)

        @Bean fun botAttemptService() = LoginAttemptService(600, 5)

        // Exercise real conditional wiring without starting native clients or network activity.
        @Bean fun lifecycleProcessor() =
            object : DefaultLifecycleProcessor() {
                override fun onRefresh() = Unit
            }
    }

    private val runner =
        ApplicationContextRunner().withUserConfiguration(
            Dependencies::class.java, BotConfig::class.java, BotStarter::class.java, TdlightConfig::class.java,
            DefaultTelegramMessageSender::class.java, MockTelegramMessageSender::class.java,
            DefaultTelegramMessageConsumer::class.java, TelegramMessageHandler::class.java,
            CallbackMenuService::class.java, UserValidatorService::class.java,
        )

    @Test fun `disabled selects only mock`() {
        runner.run { context ->
            assertFalse(context.containsBean("botStarter"))
            assertFalse(context.containsBean("tdlightTransport"))
            assertEquals(1, context.getBeansOfType(TelegramMessageSender::class.java).size)
            assertTrue(context.getBean(TelegramMessageSender::class.java) is MockTelegramMessageSender)
        }
    }

    @Test fun `default enabled selects only bot api`() {
        runner.withPropertyValues("integrations.telegram.enabled=true").run { context ->
            assertFalse(context.containsBean("tdlightTransport"))
            assertEquals(1, context.getBeansOfType(TelegramMessageSender::class.java).size)
            assertTrue(context.getBean(TelegramMessageSender::class.java) is DefaultTelegramMessageSender)
            assertEquals(1, context.getBeansOfType(BotStarter::class.java).size)
        }
    }

    @Test fun `mtproto excludes bot api and mock`() {
        runner.withPropertyValues("integrations.telegram.enabled=true", "integrations.telegram.transport=mtproto").run { context ->
            assertFalse(context.containsBean("botStarter"))
            assertTrue(context.getBeansOfType(DefaultTelegramMessageConsumer::class.java).isEmpty())
            assertEquals(1, context.getBeansOfType(TelegramMessageSender::class.java).size)
            assertTrue(context.getBean(TelegramMessageSender::class.java) is TdlightTransport)
        }
    }
}
