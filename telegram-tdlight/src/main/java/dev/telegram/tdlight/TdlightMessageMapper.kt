package dev.telegram.tdlight

import dev.telegram.api.OutgoingTelegramMessage
import dev.telegram.api.TelegramEvent
import dev.telegram.api.TelegramKeyboard
import dev.telegram.api.TelegramResponse
import it.tdlight.jni.TdApi

/** SDK conversion stays inside the transport; identifiers are never narrowed to Int. */
internal object TdlightMessageMapper {
    fun event(update: TdApi.Object): TelegramEvent? =
        when (update) {
            is TdApi.UpdateNewMessage -> {
                val message = update.message
                val sender = message.senderId as? TdApi.MessageSenderUser
                val content = message.content as? TdApi.MessageText
                if (message.isOutgoing || sender == null || content == null) {
                    null
                } else {
                    TelegramEvent(sender.userId, message.chatId, message.id, content.text.text, sentAt = message.date.toLong())
                }
            }
            is TdApi.UpdateNewCallbackQuery -> {
                val payload = update.payload as? TdApi.CallbackQueryPayloadData
                payload?.let {
                    TelegramEvent(
                        update.senderUserId,
                        update.chatId,
                        update.messageId,
                        it.data.toString(Charsets.UTF_8),
                        update.id.toString(),
                    )
                }
            }
            else -> null
        }

    fun outgoing(message: OutgoingTelegramMessage): TdApi.SendMessage {
        require(message.text.isNotBlank()) { "Message text is required" }
        val keyboard = message.keyboard
        return if (keyboard is TelegramKeyboard.Inline) {
            (response(message.chatId, TelegramResponse(message.text, keyboard.rows), null) as TdApi.SendMessage).apply {
                replyTo = message.replyToMessageId?.let { TdApi.InputMessageReplyToMessage().apply { messageId = it } }
            }
        } else {
            message(message.text, message.chatId, message.replyToMessageId, (keyboard as? TelegramKeyboard.Reply)?.rows.orEmpty())
        }
    }

    fun message(
        text: String,
        chatId: Long,
        replyId: Long?,
        buttons: List<List<String>>,
    ) = TdApi.SendMessage().apply {
        this.chatId = chatId
        inputMessageContent = textContent(text)
        if (buttons.isNotEmpty()) {
            replyMarkup =
                TdApi.ReplyMarkupShowKeyboard().apply {
                    rows =
                        buttons.map { row ->
                            row.map { label ->
                                TdApi.KeyboardButton().apply {
                                    this.text = label
                                    type = TdApi.KeyboardButtonTypeText()
                                }
                            }.toTypedArray()
                        }.toTypedArray()
                    resizeKeyboard = true
                    isPersonal = true
                    inputFieldPlaceholder = ""
                }
        }
        replyTo = replyId?.let { TdApi.InputMessageReplyToMessage().apply { messageId = it } }
    }

    fun response(
        chatId: Long,
        response: TelegramResponse,
        editId: Long?,
    ): TdApi.Function<*> {
        val markup =
            response.buttons?.let { rows ->
                TdApi.ReplyMarkupInlineKeyboard(
                    rows.map { row ->
                        row.map { button ->
                            val data = button.data.toByteArray(Charsets.UTF_8)
                            require(data.size in 1..64) { "Callback data must contain 1 to 64 bytes" }
                            TdApi.InlineKeyboardButton().apply {
                                text = button.text
                                type = TdApi.InlineKeyboardButtonTypeCallback(data)
                            }
                        }.toTypedArray()
                    }.toTypedArray(),
                    false,
                )
            }
        return if (editId == null) {
            TdApi.SendMessage().apply {
                this.chatId = chatId
                inputMessageContent = textContent(response.text)
                replyMarkup = markup
            }
        } else {
            TdApi.EditMessageText().apply {
                this.chatId = chatId
                messageId = editId
                inputMessageContent = textContent(response.text)
                replyMarkup = markup
            }
        }
    }

    fun acknowledgement(id: String) = TdApi.AnswerCallbackQuery(id.toLong(), "", false, "", 0)

    private fun textContent(text: String) = TdApi.InputMessageText().apply { this.text = TdApi.FormattedText(text, emptyArray()) }
}
