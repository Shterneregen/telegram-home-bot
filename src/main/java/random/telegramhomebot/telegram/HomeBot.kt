package random.telegramhomebot.telegram

import com.fasterxml.jackson.databind.ObjectMapper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import org.telegram.telegrambots.bots.TelegramLongPollingBot
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.objects.Message
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.exceptions.TelegramApiException
import random.telegramhomebot.const.AppConstants.UNAUTHORIZED_ACCESS_MSG
import random.telegramhomebot.services.UserValidatorService
import random.telegramhomebot.services.commands.CommandService
import random.telegramhomebot.services.messages.MessageService
import random.telegramhomebot.telegram.menu.CallbackMenuService
import random.telegramhomebot.utils.logger

@ConditionalOnProperty(prefix = "mock-bot", value = ["enabled"], havingValue = "false")
@Component
class HomeBot(
    private val userValidatorService: UserValidatorService,
    private val commandService: CommandService,
    private val messageService: MessageService,
    private val callbackMenuService: CallbackMenuService,
    private val botProperties: BotProperties
) : TelegramLongPollingBot(), Bot {
    val log = logger()

    override fun onUpdateReceived(update: Update) {
        if (log.isDebugEnabled) {
            log.debug(ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(update))
        }
        when {
            !userValidatorService.checkAccessForUpdate(update) -> logWarnMessage(update)
            update.hasCallbackQuery() -> handleCallback(update)
            getMenuForCommand(update.message) -> log.debug("[${update.message}] command was executed")
            else -> executeCommandOnMachine(update.message)
        }
    }

    private fun logWarnMessage(update: Update) {
        val (from, messageText) = when {
            update.hasCallbackQuery() -> Pair(update.callbackQuery.from, update.callbackQuery.data)
            else -> Pair(update.message.from, update.message.text)
        }
        val id = from.id
        val userName = from.userName
        val firstName = from.firstName
        val lastName = from.lastName
        val warnMessage = messageService.getMessage(
            UNAUTHORIZED_ACCESS_MSG, arrayOf(id, messageText, userName, firstName, lastName)
        )
        log.warn(warnMessage)
    }

    private fun executeCommandOnMachine(message: Message) =
        commandService.executeCommandOnMachine(message.text.lowercase())
            ?.let { sendMessage(it, message.chatId, message.messageId) }

    private fun handleCallback(update: Update) = CoroutineScope(Dispatchers.Default).launch {
        try {
            executeAsync(callbackMenuService.handleCallback(update))
                .thenApply {
                    // https://core.telegram.org/bots/api#callbackquery
                    // NOTE: After the user presses a callback button, Telegram clients will display a progress bar
                    // until you call answerCallbackQuery.
                    // It is, therefore, necessary to react by calling answerCallbackQuery even if no notification
                    // to the user is needed (e.g., without specifying any of the optional parameters).
                    executeAsync(AnswerCallbackQuery.builder().callbackQueryId(update.callbackQuery.id).build())
                }
        } catch (e: Exception) {
            log.error(e.message, e)
            executeAsync(
                AnswerCallbackQuery.builder().callbackQueryId(update.callbackQuery.id)
                    .text("${Icon.WARNING.get()} Unexpected error occurred!")
                    .build()
            )
        }
    }

    private fun getMenuForCommand(message: Message): Boolean {
        val menuForCommand = callbackMenuService.getMenuForCommand(message) ?: return false
        try {
            executeAsync(menuForCommand)
        } catch (e: TelegramApiException) {
            log.error(e.message, e)
        }
        return true
    }

    override fun sendMessage(messageText: String) = sendMessage(messageText, botProperties.botOwnerId.toLong(), null)

    private fun sendMessage(messageText: String, chatId: Long, replyToMessageId: Int? = null) {
        if (messageText.isBlank()) {
            log.debug("trying to send blank message")
            return
        }
        val message = SendMessage.builder()
            .chatId(chatId.toString())
            .text(messageText)
            .replyToMessageId(replyToMessageId)
            .replyMarkup(commandService.createReplyMarkupCommandButtons())
            .build()
        try {
            executeAsync(message)
            log.debug("Message to send: {}", message)
        } catch (e: TelegramApiException) {
            log.error(e.message, e)
        }
    }

    override fun getBotUsername() = botProperties.botName
    override fun getBotToken() = botProperties.token
}
