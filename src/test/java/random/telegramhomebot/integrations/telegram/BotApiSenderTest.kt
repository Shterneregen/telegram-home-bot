package random.telegramhomebot.integrations.telegram
import dev.telegram.api.TelegramButton
import dev.telegram.api.TelegramResponse
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.doThrow
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText
import org.telegram.telegrambots.meta.generics.TelegramClient
import random.telegramhomebot.integrations.telegram.send.DefaultTelegramMessageSender
import random.telegramhomebot.services.commands.CommandService

class BotApiSenderTest {
    private val client = mock(TelegramClient::class.java)
    private val bot =
        BotProperties().apply {
            token = "123:test"
            chatId = 42L
        }
    private val sender = DefaultTelegramMessageSender(bot, client, mock(CommandService::class.java), 3)

    @Test fun `text reply preserves target`() {
        sender.sendMessage("response", -100123L, 123L)
        val capture = ArgumentCaptor.forClass(SendMessage::class.java)
        verify(client).execute(capture.capture())
        assertEquals("-100123", capture.value.chatId)
        assertEquals(123, capture.value.replyToMessageId)
        assertEquals("response", capture.value.text)
    }

    @Test fun `inline menu callback edits the same message`() {
        sender.send(-100123L, TelegramResponse("updated", listOf(listOf(TelegramButton("Wake", "/wol1")))), 123L)
        val capture = ArgumentCaptor.forClass(EditMessageText::class.java)
        verify(client).execute(capture.capture())
        assertEquals(123, capture.value.messageId)
        assertEquals("/wol1", capture.value.replyMarkup.keyboard[0][0].callbackData)
        sender.answerCallback("9007199254740993")
        val ack = ArgumentCaptor.forClass(AnswerCallbackQuery::class.java)
        verify(client).execute(ack.capture())
        assertEquals("9007199254740993", ack.value.callbackQueryId)
    }

    @Test fun `notification reports transport failure to caller`() {
        doThrow(IllegalStateException("test failure")).`when`(client).execute(any(SendMessage::class.java))
        val result = sender.sendMessage("notification")
        assertThrows(java.util.concurrent.CompletionException::class.java) { result.join() }
    }
}
