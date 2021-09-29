package random.telegramhomebot.services.menu

import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup
import random.telegramhomebot.services.menu.dto.Menu

interface MenuService {
    val menuCommand: String
    val menuText: String
    fun getMenuMap(): Map<String, Menu>
    fun getMenuInlineKeyboardMarkup(): InlineKeyboardMarkup
}
