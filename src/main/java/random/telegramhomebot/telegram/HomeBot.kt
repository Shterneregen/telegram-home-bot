package random.telegramhomebot.telegram

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import org.telegram.telegrambots.bots.TelegramLongPollingBot
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.objects.Message
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.exceptions.TelegramApiException
import random.telegramhomebot.AppConstants.Messages.UNAUTHORIZED_ACCESS_MSG
import random.telegramhomebot.config.ProfileService
import random.telegramhomebot.services.UserValidatorService
import random.telegramhomebot.services.commands.CommandService
import random.telegramhomebot.services.menu.CallbackMenuService
import random.telegramhomebot.services.messages.MessageService
import random.telegramhomebot.utils.logger

@Profile("!${ProfileService.MOCK_BOT}")
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
        log.debug(update.toString())
        when {
            !userValidatorService.checkAccessForUpdate(update) -> logWarnMessage(update)
            update.hasCallbackQuery() -> processCallback(update)
            executeControlCommand(update.message) -> log.debug("Control command [${update.message}] was executed")
            else -> executeCommand(update.message)
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

    private fun executeCommand(message: Message) = commandService.executeCommandOnMachine(message.text.lowercase())
        ?.let { sendMessage(it, message.chatId, message.messageId) }

    private fun processCallback(update: Update) = CoroutineScope(Dispatchers.Default).launch {
        try {
            execute(callbackMenuService.processCallback(update))
        } catch (e: TelegramApiException) {
            log.error(e.message, e)
        }
    }

    private fun executeControlCommand(message: Message): Boolean {
        val menuForCommand = callbackMenuService.getMenuForCommand(message) ?: return false
        try {
            execute(menuForCommand)
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
            .build()
        commandService.setCommandButtons(message)
        try {
            execute(message)
            log.debug("Message to send: {}", message)
        } catch (e: TelegramApiException) {
            log.error(e.message, e)
        }
    }

    override fun getBotUsername() = botProperties.botName
    override fun getBotToken() = botProperties.token
}
