package random.telegramhomebot.integrations.telegram.consume

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Service
import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.api.objects.message.Message
import random.telegramhomebot.const.AppConstants.UNAUTHORIZED_ACCESS_MSG
import random.telegramhomebot.integrations.telegram.Icon
import random.telegramhomebot.integrations.telegram.menu.CallbackMenuService
import random.telegramhomebot.integrations.telegram.send.TelegramMessageSender
import random.telegramhomebot.services.UserValidatorService
import random.telegramhomebot.services.commands.CommandService
import random.telegramhomebot.services.messages.MessageService
import random.telegramhomebot.utils.logger

@Service
@ConditionalOnProperty(name = ["integrations.telegram.enabled"], havingValue = "true")
class DefaultTelegramMessageConsumer(
    private val userValidatorService: UserValidatorService,
    private val commandService: CommandService,
    private val messageService: MessageService,
    private val callbackMenuService: CallbackMenuService,
    private val telegramMessageSender: TelegramMessageSender
) : LongPollingSingleThreadUpdateConsumer, TelegramMessageConsumer {
    private val log = logger()

    override fun consume(update: Update) {
        if (log.isDebugEnabled) {
            log.debug(ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(update))
        }
        when {
            !userValidatorService.checkAccessForUpdate(update) -> logWarnMessage(update)
            update.hasCallbackQuery() -> handleCallback(update)
            getMenuForCommand(update.message) -> log.debug("[{}] command was executed", update.message)
            else -> executeCommandOnMachine(update.message)
        }
    }

    private fun executeCommandOnMachine(message: Message) =
        commandService.executeCommandOnMachine(message.text.lowercase())
            ?.let { telegramMessageSender.sendMessage(it, message.chatId, message.messageId) }

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

    private fun handleCallback(update: Update) {
        val response = callbackMenuService.handleCallback(update)
        telegramMessageSender.execute(response)
            ?.let {
                telegramMessageSender.execute(
                    AnswerCallbackQuery.builder()
                        .callbackQueryId(update.callbackQuery.id)
                        .build()
                )
            }
            ?: telegramMessageSender.execute(
                AnswerCallbackQuery.builder()
                    .callbackQueryId(update.callbackQuery.id)
                    .text("${Icon.WARNING.get()} Unexpected error occurred!")
                    .build()
            )
    }

    private fun getMenuForCommand(message: Message): Boolean =
        callbackMenuService.getMenuForCommand(message)
            ?.let { menu ->
                telegramMessageSender.execute(menu) != null
            } ?: false
}