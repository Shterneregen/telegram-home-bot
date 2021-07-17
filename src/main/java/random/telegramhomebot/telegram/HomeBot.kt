package random.telegramhomebot.telegram

import org.apache.logging.log4j.util.Strings
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import org.telegram.telegrambots.bots.TelegramLongPollingBot
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.objects.Message
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.exceptions.TelegramApiException
import random.telegramhomebot.AppConstants.Messages.UNAUTHORIZED_ACCESS_MSG
import random.telegramhomebot.config.ProfileService
import random.telegramhomebot.services.CommandService
import random.telegramhomebot.services.MessageService
import random.telegramhomebot.services.UserValidatorService
import random.telegramhomebot.services.menu.CallbackMenuService
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
            !checkAccessForUpdate(update) -> sendWarnMessage(update)
            update.hasCallbackQuery() -> processCallback(update)
            executeControlCommand(update.message) -> {
            }
            else -> executeCommand(update.message)
        }
    }

    private fun sendWarnMessage(update: Update) {
        val id: Long
        val messageText: String
        val userName: String
        val firstName: String
        val lastName: String
        if (update.hasCallbackQuery()) {
            val from = update.callbackQuery.from
            id = from.id
            userName = from.userName
            firstName = from.firstName
            lastName = from.lastName
            messageText = update.callbackQuery.data
        } else {
            val from = update.message.from
            id = from.id
            userName = from.userName
            firstName = from.firstName
            lastName = from.lastName
            messageText = update.message.text
        }
        val warnMessage = messageService.getMessage(
            UNAUTHORIZED_ACCESS_MSG, arrayOf(id, messageText, userName, firstName, lastName)
        )
        log.warn(warnMessage)
        sendMessage(warnMessage)
    }

    private fun executeCommand(message: Message) {
        val commandOutput = commandService.executeCommandOnMachine(message.text.lowercase())
        if (commandOutput != null) {
            sendMessage(java.lang.String.join("\n", commandOutput), message.chatId, message.messageId)
        }
    }

    private fun checkAccessForUpdate(update: Update): Boolean {
        if (!update.hasCallbackQuery() && (!update.hasMessage() || !update.message.hasText())) {
            return false
        }
        val userId = if (update.hasCallbackQuery()) update.callbackQuery.from.id else update.message.from.id
        return userValidatorService.isAllowedUser(userId)
    }

    private fun processCallback(update: Update) {
        Thread {
            try {
                execute(callbackMenuService.processCallback(update))
            } catch (e: TelegramApiException) {
                log.error(e.message, e)
            }
        }.start()
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
        if (Strings.isBlank(messageText)) {
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