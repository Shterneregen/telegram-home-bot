package random.telegramhomebot.bootstrap

import org.springframework.boot.CommandLineRunner
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import random.telegramhomebot.const.AppConstants.CHATBOT_STARTED_MSG
import random.telegramhomebot.services.messages.MessageService
import random.telegramhomebot.integrations.telegram.send.TelegramMessageSender
import random.telegramhomebot.utils.logger

@Order(99)
@Component
class BootstrapLoader(
    private val telegramMessageSender: TelegramMessageSender,
    private val messageService: MessageService
) : CommandLineRunner {
    val log = logger()

    override fun run(vararg args: String) {
        log.info("BootstrapLoader started")
        sendBootstrapMessage()
    }

    private fun sendBootstrapMessage() {
        telegramMessageSender.sendMessage(messageService.getMessage(CHATBOT_STARTED_MSG))
    }
}
