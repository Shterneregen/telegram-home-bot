package random.telegramhomebot.integrations.telegram

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.springframework.boot.actuate.health.HealthIndicator
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.support.DefaultLifecycleProcessor
import random.telegramhomebot.auth.services.LoginAttemptService
import random.telegramhomebot.db.model.TelegramCommand
import random.telegramhomebot.integrations.telegram.consume.DefaultTelegramMessageConsumer
import random.telegramhomebot.integrations.telegram.consume.TelegramMessageHandler
import random.telegramhomebot.integrations.telegram.menu.CallbackMenuService
import random.telegramhomebot.integrations.telegram.mtproto.TdlightConfig
import random.telegramhomebot.integrations.telegram.mtproto.TdlightTransport
import random.telegramhomebot.integrations.telegram.mtproto.MtprotoProperties
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
            assertTrue(context.getBeansOfType(TelegramEventHandler::class.java).isEmpty())
            assertTrue(context.getBeansOfType(MtprotoProperties::class.java).isEmpty())
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
            assertEquals(1, context.getBeansOfType(DefaultTelegramMessageConsumer::class.java).size)
            assertTrue(context.getBeansOfType(TelegramEventHandler::class.java).isEmpty())
            assertTrue(context.getBeansOfType(MtprotoProperties::class.java).isEmpty())
        }
    }

    @Test fun `disabled mtproto selection creates neither native transport nor bound settings`() {
        runner.withPropertyValues("integrations.telegram.enabled=false", "integrations.telegram.transport=mtproto").run { context ->
            assertFalse(context.containsBean("tdlightTransport"))
            assertFalse(context.containsBean("botStarter"))
            assertTrue(context.getBeansOfType(MtprotoProperties::class.java).isEmpty())
            assertTrue(context.getBeansOfType(TelegramEventHandler::class.java).isEmpty())
            assertEquals(1, context.getBeansOfType(TelegramMessageSender::class.java).size)
            assertTrue(context.getBean(TelegramMessageSender::class.java) is MockTelegramMessageSender)
        }
    }

    @Test fun `explicit bot api selection has one sender and consumer`() {
        runner.withPropertyValues("integrations.telegram.enabled=true", "integrations.telegram.transport=bot-api").run { context ->
            assertFalse(context.containsBean("tdlightTransport"))
            assertTrue(context.getBeansOfType(MtprotoProperties::class.java).isEmpty())
            assertEquals(1, context.getBeansOfType(TelegramMessageSender::class.java).size)
            assertTrue(context.getBean(TelegramMessageSender::class.java) is DefaultTelegramMessageSender)
            assertEquals(1, context.getBeansOfType(DefaultTelegramMessageConsumer::class.java).size)
            assertEquals(1, context.getBeansOfType(BotStarter::class.java).size)
        }
    }

    @Test fun `mtproto excludes bot api and mock`() {
        runner.withPropertyValues("integrations.telegram.enabled=true", "integrations.telegram.transport=mtproto").run { context ->
            assertFalse(context.containsBean("botStarter"))
            assertTrue(context.getBeansOfType(DefaultTelegramMessageConsumer::class.java).isEmpty())
            assertEquals(1, context.getBeansOfType(TelegramMessageSender::class.java).size)
            assertTrue(context.getBean(TelegramMessageSender::class.java) is TdlightTransport)
            assertTrue(context.getBeansOfType(HealthIndicator::class.java).containsKey("tdlightTransport"))
            assertEquals(1, context.getBeansOfType(TelegramEventHandler::class.java).size)
        }
    }

    @Test fun `mtproto adapter uses authorized handler and current command button labels`() {
        runner.withPropertyValues(
            "integrations.telegram.enabled=true", "integrations.telegram.transport=mtproto",
            "integrations.telegram.mtproto.api-id=12345",
            "integrations.telegram.mtproto.proxy-port=8443",
        ).run { context ->
            val properties = context.getBean(MtprotoProperties::class.java)
            assertEquals(12345, properties.apiId)
            assertEquals(8443, properties.proxyPort)
            val commands = context.getBean(CommandService::class.java)
            @Suppress("UNCHECKED_CAST")
            val buttons = context.getBean("tdlightCommandButtons") as () -> List<String>
            `when`(commands.getAllEnabledCommands()).thenReturn(listOf(TelegramCommand("/wake", "test", true)))
            assertEquals(listOf("/wake"), buttons())
            `when`(commands.getAllEnabledCommands()).thenReturn(emptyList())
            assertTrue(buttons().isEmpty())

            val handler = context.getBean(TelegramEventHandler::class.java)
            handler.handle(TelegramEvent(42, 42, 123, "/test"))
            verify(commands).executeCommandOnMachine("/test")
        }
    }

    @Test fun `event adapter forwards preacknowledged callback without another ACK`() {
        val sender = mock(TelegramMessageSender::class.java)
        runner.withPropertyValues("integrations.telegram.enabled=true", "integrations.telegram.transport=mtproto")
            .withBean("testSender", TelegramMessageSender::class.java, { sender }, { it.isPrimary = true })
            .run { context ->
                val handler = context.getBean(TelegramEventHandler::class.java)
                handler.handle(TelegramEvent(99, 42, 123, "/menu", "callback-denied"))
                verifyNoInteractions(sender, context.getBean(CommandService::class.java))
                handler.handle(TelegramEvent(42, 42, 123, "/menu", "callback-allowed"))
                verify(sender).send(42, TelegramResponse("No answer"), 123)
                org.mockito.Mockito.verifyNoMoreInteractions(sender)
            }
    }
}
