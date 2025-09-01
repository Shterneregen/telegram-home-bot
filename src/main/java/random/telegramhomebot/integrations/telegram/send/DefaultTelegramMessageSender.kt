package random.telegramhomebot.integrations.telegram.send

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Service
import org.telegram.telegrambots.meta.api.methods.botapimethods.BotApiMethod
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.exceptions.TelegramApiException
import org.telegram.telegrambots.meta.generics.TelegramClient
import random.telegramhomebot.integrations.telegram.BotProperties
import random.telegramhomebot.services.commands.CommandService
import random.telegramhomebot.utils.logger
import java.io.Serializable

@Service
@ConditionalOnProperty(name = ["integrations.telegram.enabled"], havingValue = "true")
class DefaultTelegramMessageSender(
    private val botProperties: BotProperties,
    private val telegramClient: TelegramClient,
    private val commandService: CommandService
) : TelegramMessageSender {
    private val log = logger()

    override fun sendMessage(messageText: String) = sendMessage(messageText, botProperties.chatId.toLong())

    override fun sendMessage(messageText: String, chatId: Long, replyToMessageId: Int?) {
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

        log.debug("Message to send: {}", message)
        execute(message)
    }

    override fun <T : Serializable, M : BotApiMethod<T>> execute(method: M): T? {
        try {
            return telegramClient.execute(method)
        } catch (e: TelegramApiException) {
            log.error("Error: {}", e.message, e)
            return null
        }
    }
}