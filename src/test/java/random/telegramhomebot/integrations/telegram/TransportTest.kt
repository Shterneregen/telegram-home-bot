package random.telegramhomebot.integrations.telegram
import dev.telegram.api.TelegramEvent
import dev.telegram.api.TelegramResponse
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import random.telegramhomebot.auth.services.LoginAttemptService
import random.telegramhomebot.integrations.telegram.consume.TelegramMessageHandler
import random.telegramhomebot.integrations.telegram.menu.CallbackMenuService
import random.telegramhomebot.integrations.telegram.mtproto.MtprotoProperties
import random.telegramhomebot.integrations.telegram.send.TelegramMessageSender
import random.telegramhomebot.services.UserValidatorService
import random.telegramhomebot.services.commands.CommandService

class TransportTest {
    private fun settings() =
        MtprotoProperties().apply {
            apiId = 12345
            apiHash = "a".repeat(32)
            proxyHost = "proxy.example.invalid"
            proxySecret = "ee" + "a".repeat(32) + "6578616d706c652e636f6d"
        }

    @Test fun `full FakeTLS secret is preserved`() {
        val settings = settings()
        val secret = settings.proxySecret
        settings.validate()
        assertEquals(secret, settings.proxySecret)
    }

    @Test fun `invalid proxy and credentials are rejected`() {
        val changes: List<(MtprotoProperties) -> Unit> =
            listOf(
                { it.apiId = 0 },
                { it.apiHash = "" },
                { it.proxyHost = "" },
                { it.proxyPort = 0 },
                { it.proxyPort = 65536 },
                { it.proxySecret = "ee" + "a".repeat(32) },
                { it.proxySecret = "dd" + "a".repeat(32) },
                { it.proxySecret = "secret" },
            )
        changes.forEach { change -> assertThrows(IllegalArgumentException::class.java) { settings().also(change).validate() } }
    }

    private fun bot() =
        BotProperties().apply {
            token = "123:test"
            chatId = 42L
            homeGroupUserIds = listOf(43L)
        }

    @Test fun `transport validates and defaults to bot api`() {
        val bot = bot()
        assertEquals("bot-api", bot.transport)
        bot.validate()
        bot.transport = "mtproto"
        bot.validate()
        bot.transport = "unknown"
        assertThrows(IllegalArgumentException::class.java) { bot.validate() }
    }

    @Test fun `missing bot token is rejected`() {
        assertThrows(IllegalArgumentException::class.java) { bot().apply { token = "" }.validate() }
    }

    @Test fun `authorized callback routes to current menu and is acknowledged once`() {
        val commands = mock(CommandService::class.java)
        val menus = mock(CallbackMenuService::class.java)
        val sender = mock(TelegramMessageSender::class.java)
        val response = TelegramResponse("updated")
        org.mockito.Mockito.`when`(menus.getRequestResult("/features")).thenReturn(response)
        val handler = TelegramMessageHandler(UserValidatorService(bot(), LoginAttemptService(600, 5)), commands, menus, sender)
        val callback = TelegramEvent(43, -10042, 1L shl 35, "/features", "99")
        handler.consume(callback)
        handler.consume(callback)
        verify(sender).answerCallback("99")
        verify(sender).send(callback.chatId, response, callback.messageId)
        verifyNoInteractions(commands)
    }

    @Test fun `authorization uses user identity and preserves blocking`() {
        val attempts = LoginAttemptService(600, 5)
        val validator = UserValidatorService(bot(), attempts)
        assertTrue(validator.checkAccess(42))
        assertTrue(validator.checkAccess(43))
        repeat(5) { assertFalse(validator.checkAccess(99)) }
        assertTrue(attempts.isBlocked("99"))
        assertFalse(validator.checkAccess(-42))
        repeat(5) { attempts.loginFailed("42") }
        assertFalse(validator.checkAccess(42))
    }

    @Test fun `denied text and callbacks never execute actions`() {
        val commands = mock(CommandService::class.java)
        val menus = mock(CallbackMenuService::class.java)
        val sender = mock(TelegramMessageSender::class.java)
        val handler = TelegramMessageHandler(UserValidatorService(bot(), LoginAttemptService(600, 5)), commands, menus, sender)
        handler.consume(TelegramEvent(99, 42, 1, "/danger"))
        handler.consume(TelegramEvent(99, 42, 2, "/wol", "7"))
        verifyNoInteractions(commands, menus)
        verify(sender).answerCallback("7")
    }

    @Test fun `preacknowledged callback still requires sender authorization`() {
        val commands = mock(CommandService::class.java)
        val menus = mock(CallbackMenuService::class.java)
        val sender = mock(TelegramMessageSender::class.java)
        val handler = TelegramMessageHandler(UserValidatorService(bot(), LoginAttemptService(600, 5)), commands, menus, sender)
        handler.consumeAcknowledged(TelegramEvent(99, 42, 2, "/wol", "7"))
        verifyNoInteractions(commands, menus, sender)
    }

    @Test fun `duplicate authorized text executes once`() {
        val commands = mock(CommandService::class.java)
        val sender = mock(TelegramMessageSender::class.java)
        val handler =
            TelegramMessageHandler(
                UserValidatorService(bot(), LoginAttemptService(600, 5)),
                commands,
                CallbackMenuService(emptyList()),
                sender,
            )
        val event = TelegramEvent(42, 42, 1L shl 35, "/test")
        handler.consume(event)
        handler.consume(event)
        verify(commands, times(1)).executeCommandOnMachine("/test")
    }

    @Test fun `stale commands never execute`() {
        val commands = mock(CommandService::class.java)
        val sender = mock(TelegramMessageSender::class.java)
        val handler =
            TelegramMessageHandler(
                UserValidatorService(bot(), LoginAttemptService(600, 5)),
                commands,
                CallbackMenuService(emptyList()),
                sender,
            )
        handler.consume(TelegramEvent(42, 42, 100, "/test", sentAt = 1))
        verifyNoInteractions(commands, sender)
    }
}
