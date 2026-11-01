package dev.telegram.tdlight

import dev.telegram.api.TelegramButton
import dev.telegram.api.TelegramResponse
import it.tdlight.jni.TdApi
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class TdlightMessageMapperTest {
    private val messageId = 1L shl 42
    private val chatId = -1001234567890L

    @Test fun `incoming text uses sender user not group identity`() {
        val message =
            TdApi.Message().apply {
                id = messageId
                chatId = this@TdlightMessageMapperTest.chatId
                senderId = TdApi.MessageSenderUser(42)
                date = 12345
                content = TdApi.MessageText().apply { text = TdApi.FormattedText("/wol", emptyArray()) }
            }
        val update = TdApi.UpdateNewMessage(message)
        val event = requireNotNull(TdlightMessageMapper.event(update))
        assertEquals(42L, event.userId)
        assertEquals(chatId, event.chatId)
        assertEquals(messageId, event.messageId)
        assertEquals(12345L, event.sentAt)
        message.isOutgoing = true
        assertNull(TdlightMessageMapper.event(update))
        message.isOutgoing = false
        message.senderId = TdApi.MessageSenderChat(chatId)
        assertNull(TdlightMessageMapper.event(update))
        assertNull(TdlightMessageMapper.event(TdApi.Ok()))
    }

    @Test fun `callback sender payload and signed identifier survive mapping`() {
        val query =
            TdApi.UpdateNewCallbackQuery().apply {
                id = Long.MIN_VALUE + 42
                senderUserId = 42
                chatId = this@TdlightMessageMapperTest.chatId
                messageId = this@TdlightMessageMapperTest.messageId
                payload = TdApi.CallbackQueryPayloadData("/features".toByteArray())
            }
        val event = requireNotNull(TdlightMessageMapper.event(query))
        assertEquals(42L, event.userId)
        assertEquals("/features", event.text)
        assertEquals(messageId, event.messageId)
        assertEquals(query.id, TdlightMessageMapper.acknowledgement(requireNotNull(event.callbackId)).callbackQueryId)
    }

    @Test fun `text reply and reply keyboard preserve native identifiers`() {
        val request = TdlightMessageMapper.message("response", chatId, messageId, listOf(listOf("Wake", "Weather")))
        assertEquals(chatId, request.chatId)
        assertEquals(messageId, (request.replyTo as TdApi.InputMessageReplyToMessage).messageId)
        assertEquals("response", (request.inputMessageContent as TdApi.InputMessageText).text.text)
        assertEquals("Weather", (request.replyMarkup as TdApi.ReplyMarkupShowKeyboard).rows[0][1].text)
    }

    @Test fun `inline send and edit preserve callback bytes`() {
        val response = TelegramResponse("menu", listOf(listOf(TelegramButton("Wake", "/wol1"))))
        val sent = TdlightMessageMapper.response(chatId, response, null) as TdApi.SendMessage
        val edited = TdlightMessageMapper.response(chatId, response, messageId) as TdApi.EditMessageText
        assertEquals(chatId, sent.chatId)
        assertEquals(messageId, edited.messageId)
        val button = (edited.replyMarkup as TdApi.ReplyMarkupInlineKeyboard).rows[0][0]
        assertArrayEquals("/wol1".toByteArray(), (button.type as TdApi.InlineKeyboardButtonTypeCallback).data)
        assertThrows(IllegalArgumentException::class.java) {
            TdlightMessageMapper.response(chatId, TelegramResponse("menu", listOf(listOf(TelegramButton("invalid", "я".repeat(33))))), null)
        }
    }
}
