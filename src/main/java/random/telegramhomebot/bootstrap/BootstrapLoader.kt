package random.telegramhomebot.bootstrap

import org.springframework.boot.CommandLineRunner
import org.springframework.stereotype.Component
import random.telegramhomebot.AppConstants
import random.telegramhomebot.services.MessageService
import random.telegramhomebot.telegram.Bot

@Component
class BootstrapLoader(
    private val bot: Bot,
    private val messageService: MessageService
) : CommandLineRunner {

    override fun run(vararg args: String) {
        sendBootstrapMessage()
    }

    private fun sendBootstrapMessage() {
        bot.sendMessage(messageService.getMessage(AppConstants.Messages.CHATBOT_STARTED_MSG))
    }
}
