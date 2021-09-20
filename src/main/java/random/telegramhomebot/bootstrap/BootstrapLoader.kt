package random.telegramhomebot.bootstrap

import org.springframework.boot.CommandLineRunner
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import random.telegramhomebot.AppConstants
import random.telegramhomebot.services.MessageService
import random.telegramhomebot.telegram.Bot
import random.telegramhomebot.utils.logger

@Order(99)
@Component
class BootstrapLoader(
    private val bot: Bot,
    private val messageService: MessageService
) : CommandLineRunner {
    val log = logger()

    override fun run(vararg args: String) {
        log.info("BootstrapLoader started")
        sendBootstrapMessage()
    }

    private fun sendBootstrapMessage() {
        bot.sendMessage(messageService.getMessage(AppConstants.Messages.CHATBOT_STARTED_MSG))
    }
}
