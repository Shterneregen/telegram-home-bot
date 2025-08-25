package random.telegramhomebot.integrations.telegram.menu

import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow
import random.telegramhomebot.integrations.telegram.menu.dto.Menu

interface MenuService {
    val menuCommand: String
    val menuText: String
    fun getMenuMap(): Map<String, Menu>
    fun getMenuInlineKeyboardMarkup(): InlineKeyboardMarkup

    fun getDefaultHorizontalMenuInlineKeyboardMarkup(): InlineKeyboardMarkup {
        return getDefaultMenuInlineKeyboardMarkup()
    }

    fun getDefaultVerticalMenuInlineKeyboardMarkup(): InlineKeyboardMarkup {
        return getDefaultMenuInlineKeyboardMarkup(MenuOrientation.VERTICAL)
    }

    fun getDefaultMenuInlineKeyboardMarkup(
        orientation: MenuOrientation = MenuOrientation.HORIZONTAL
    ): InlineKeyboardMarkup {
        val buttons = getMenuMap().entries.map { (command, menu) ->
            InlineKeyboardButton.builder()
                .text(menu.buttonText)
                .callbackData(command)
                .build()
        }

        val rows = when (orientation) {
            MenuOrientation.HORIZONTAL -> listOf(InlineKeyboardRow(buttons)) // все кнопки в один ряд
            MenuOrientation.VERTICAL -> buttons.map { InlineKeyboardRow(listOf(it)) } // каждая кнопка в отдельный ряд
        }

        return InlineKeyboardMarkup.builder()
            .keyboard(rows)
            .build()
    }
}
