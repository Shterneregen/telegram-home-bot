package random.telegramhomebot.services.menu

import org.springframework.stereotype.Service
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText
import org.telegram.telegrambots.meta.api.objects.Message
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup
import random.telegramhomebot.AppConstants.BotCommands

@Service
class CallbackMenuService(
    private val mainMenuService: MainMenuService,
    private val featuresMenuService: FeaturesMenuService
) {

    fun processCallback(update: Update): EditMessageText {
        val requestResult = getRequestResult(update.callbackQuery.data)
        return EditMessageText.builder()
            .chatId(update.callbackQuery.message.chatId.toString())
            .messageId(update.callbackQuery.message.messageId)
            .replyMarkup(requestResult.inlineKeyboardMarkup)
            .text(requestResult.answer)
            .build()
    }

    fun getRequestResult(callData: String): RequestResult {
        val menu = mainMenuService.getMainMenuMap()[callData]
        if (menu != null) {
            return RequestResult(menu.method.get(), mainMenuService.getMainMenuInlineKeyboardMarkup())
        }
        val featureMenu = featuresMenuService.getFeaturesMenuMap()[callData]
        if (featureMenu != null) {
            return RequestResult(featureMenu.method.get(), featuresMenuService.getFeaturesMenuInlineKeyboardMarkup())
        }
        return RequestResult("No answer", null)
    }

    fun getMenuForCommand(message: Message): SendMessage? {
        val chatId = message.chatId
        return when (message.text) {
            BotCommands.MENU_COMMAND -> getInlineKeyBoardMessage(
                chatId, "Options", mainMenuService.getMainMenuInlineKeyboardMarkup()
            )
            BotCommands.FEATURES -> getInlineKeyBoardMessage(
                chatId, "Features Settings", featuresMenuService.getFeaturesMenuInlineKeyboardMarkup()
            )
            else -> {
                null
            }
        }
    }

    private fun getInlineKeyBoardMessage(chatId: Long, text: String, inlineKeyboardMarkup: InlineKeyboardMarkup)
            : SendMessage = SendMessage.builder()
        .chatId(chatId.toString())
        .text(text)
        .replyMarkup(inlineKeyboardMarkup)
        .build()
}

class RequestResult(var answer: String, var inlineKeyboardMarkup: InlineKeyboardMarkup? = null)
