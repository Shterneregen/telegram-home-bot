package random.telegramhomebot.integrations.telegram.menu

import dev.telegram.api.TelegramButton
import random.telegramhomebot.integrations.telegram.menu.dto.Menu

interface MenuService {
    val menuCommand: String
    val menuText: String

    fun getMenuMap(): Map<String, Menu>

    fun getMenuInlineKeyboardMarkup(): List<List<TelegramButton>>

    fun getDefaultHorizontalMenuInlineKeyboardMarkup() = getDefaultMenuInlineKeyboardMarkup()

    fun getDefaultVerticalMenuInlineKeyboardMarkup() = getDefaultMenuInlineKeyboardMarkup(MenuOrientation.VERTICAL)

    fun getDefaultMenuInlineKeyboardMarkup(orientation: MenuOrientation = MenuOrientation.HORIZONTAL): List<List<TelegramButton>> {
        val buttons = getMenuMap().map { (key, menu) -> TelegramButton(menu.buttonText, key) }
        return if (orientation == MenuOrientation.HORIZONTAL) listOf(buttons) else buttons.map { listOf(it) }
    }
}
