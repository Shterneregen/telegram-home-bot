package random.telegramhomebot.services.menu

import org.springframework.stereotype.Service
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText
import org.telegram.telegrambots.meta.api.objects.Message
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup

@Service
class CallbackMenuService(private val menuServices: List<MenuService>) {

    fun processCallback(update: Update): EditMessageText {
        val requestResult = getRequestResult(update.callbackQuery.data)
        return EditMessageText.builder()
            .chatId(update.callbackQuery.message.chatId.toString())
            .messageId(update.callbackQuery.message.messageId)
            .replyMarkup(requestResult.inlineKeyboardMarkup)
            .text(requestResult.answer)
            .build()
    }

    fun getRequestResult(command: String): RequestResult {
        for (menuService in menuServices) {
            menuService.getMenuMap()[command]
                ?.let { return RequestResult(it.method.get(), menuService.getMenuInlineKeyboardMarkup()) }
        }
        return RequestResult("No answer")
    }

    fun getMenuForCommand(message: Message): SendMessage? =
        menuServices.find { message.text.equals(it.menuCommand) }
            ?.let { getInlineKeyBoardMessage(message.chatId, it.menuText, it.getMenuInlineKeyboardMarkup()) }

    private fun getInlineKeyBoardMessage(
        chatId: Long,
        text: String,
        inlineKeyboardMarkup: InlineKeyboardMarkup
    ): SendMessage =
        SendMessage.builder()
            .chatId(chatId.toString())
            .text(text)
            .replyMarkup(inlineKeyboardMarkup)
            .build()

    class RequestResult(var answer: String, var inlineKeyboardMarkup: InlineKeyboardMarkup? = null)
}
