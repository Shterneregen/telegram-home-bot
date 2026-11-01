package random.telegramhomebot.integrations.telegram.send
import dev.telegram.api.TelegramResponse
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow
import org.telegram.telegrambots.meta.generics.TelegramClient
import random.telegramhomebot.integrations.telegram.BotApiTransport
import random.telegramhomebot.integrations.telegram.BotProperties
import random.telegramhomebot.services.commands.CommandService
import random.telegramhomebot.utils.logger
import java.util.concurrent.CompletableFuture

@Service
@BotApiTransport
class DefaultTelegramMessageSender(
    private val properties: BotProperties,
    private val client: TelegramClient,
    private val commands: CommandService,
    @Value("\${buttons.in.row:3}") private val buttonsInRow: Int,
) : TelegramMessageSender {
    private val log = logger()

    override fun sendMessage(messageText: String): CompletableFuture<Void> = sendText(messageText, properties.chatId.toLong(), null)

    override fun sendMessage(
        messageText: String,
        chatId: Long,
        replyToMessageId: Long?,
    ) {
        sendText(messageText, chatId, replyToMessageId)
    }

    private fun sendText(
        messageText: String,
        chatId: Long,
        replyToMessageId: Long?,
    ): CompletableFuture<Void> {
        if (messageText.isBlank()) return CompletableFuture.completedFuture(null)
        val rows =
            commands.getAllEnabledCommands().map { KeyboardButton(it.getButtonName()) }
                .chunked(buttonsInRow.coerceAtLeast(1)).map { KeyboardRow(it) }
        return try {
            client.execute(
                SendMessage.builder().chatId(chatId.toString()).text(messageText)
                    .replyToMessageId(replyToMessageId?.let { Math.toIntExact(it) })
                    .replyMarkup(
                        if (rows.isEmpty()) {
                            null
                        } else {
                            ReplyKeyboardMarkup.builder().keyboard(rows)
                                .resizeKeyboard(true).selective(true).oneTimeKeyboard(false).build()
                        },
                    ).build(),
            )
            CompletableFuture.completedFuture(null)
        } catch (e: Exception) {
            log.warn("Telegram operation failed ({})", e.javaClass.simpleName)
            CompletableFuture.failedFuture(e)
        }
    }

    override fun send(
        chatId: Long,
        response: TelegramResponse,
        editMessageId: Long?,
    ) {
        val keyboard =
            response.buttons?.let { rows ->
                InlineKeyboardMarkup.builder().keyboard(
                    rows.map { row ->
                        InlineKeyboardRow(row.map { InlineKeyboardButton.builder().text(it.text).callbackData(it.data).build() })
                    },
                ).build()
            }
        safely {
            if (editMessageId == null) {
                client.execute(SendMessage.builder().chatId(chatId.toString()).text(response.text).replyMarkup(keyboard).build())
            } else {
                client.execute(
                    EditMessageText.builder().chatId(
                        chatId.toString(),
                    ).messageId(Math.toIntExact(editMessageId)).text(response.text).replyMarkup(keyboard).build(),
                )
            }
        }
    }

    override fun answerCallback(id: String) {
        safely { client.execute(AnswerCallbackQuery.builder().callbackQueryId(id).build()) }
    }

    private fun safely(action: () -> Any?) {
        try {
            action()
        } catch (e: Exception) {
            log.warn("Telegram operation failed ({})", e.javaClass.simpleName)
        }
    }
}
