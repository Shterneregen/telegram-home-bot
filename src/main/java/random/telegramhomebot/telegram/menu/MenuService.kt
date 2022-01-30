package random.telegramhomebot.telegram.menu

import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton
import random.telegramhomebot.telegram.menu.dto.Menu

interface MenuService {
    val menuCommand: String
    val menuText: String
    fun getMenuMap(): Map<String, Menu>
    fun getMenuInlineKeyboardMarkup(): InlineKeyboardMarkup

    fun getDefaultHorizontalMenuInlineKeyboardMarkup(): InlineKeyboardMarkup {
        val buttons = getMenuMap().entries.map { (command, menu) ->
            InlineKeyboardButton.builder()
                .text(menu.buttonText)
                .callbackData(command).build()
        }
        return InlineKeyboardMarkup.builder().keyboard(listOf(buttons)).build()
    }

    fun getDefaultVerticalMenuInlineKeyboardMarkup(): InlineKeyboardMarkup {
        val rowList: List<List<InlineKeyboardButton>> = getMenuMap()
            .map { (command, menu) ->
                InlineKeyboardButton.builder()
                    .text(menu.buttonText)
                    .callbackData(command).build()
            }.map { listOf(it) }
        return InlineKeyboardMarkup.builder().keyboard(rowList).build()
    }
}
