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
        val callbackQuery = update.callbackQuery
        return getRequestResult(callbackQuery.data).let { requestResult ->
            EditMessageText.builder()
                .chatId(callbackQuery.message.chatId.toString())
                .messageId(callbackQuery.message.messageId)
                .replyMarkup(requestResult.inlineKeyboardMarkup)
                .text(requestResult.answer)
                .build()
        }
    }

    fun getRequestResult(command: String): RequestResult =
        menuServices.find { it.getMenuMap()[command] != null }?.let { menuService ->
            menuService.getMenuMap()[command]
                ?.let { menu -> RequestResult(menu.method.get(), menuService.getMenuInlineKeyboardMarkup()) }
        } ?: RequestResult("No answer")

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
            .allowSendingWithoutReply(true)
            .build()

    class RequestResult(var answer: String, var inlineKeyboardMarkup: InlineKeyboardMarkup? = null)
}
