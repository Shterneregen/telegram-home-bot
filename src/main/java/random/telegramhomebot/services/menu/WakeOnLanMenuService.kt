package random.telegramhomebot.services.menu

import org.springframework.stereotype.Service
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton
import random.telegramhomebot.services.HostService
import random.telegramhomebot.services.WakeOnLanService
import random.telegramhomebot.services.menu.dto.Menu

@Service
class WakeOnLanMenuService(
    private val hostService: HostService,
    private val wakeOnLanService: WakeOnLanService
) : MenuService {
    override val menuCommand = "/wol"
    override val menuText = "Wake On Lan hosts"

    override fun getMenuMap(): Map<String, Menu> {
        return hostService.getWakeOnLanEnableHosts()
            .associate {
                "$menuCommand${it.id}" to Menu(it.deviceName ?: it.mac!!)
                {
                    wakeOnLanService.wakeOnLan(it.mac)
                    "Host '${it.deviceName}' start request sent"
                }
            }
    }

    override fun getMenuInlineKeyboardMarkup(): InlineKeyboardMarkup {
        val rowList: List<List<InlineKeyboardButton>> = getMenuMap().entries
            .map { (command, menu) ->
                InlineKeyboardButton.builder()
                    .text("Start '${menu.buttonText}'")
                    .callbackData(command).build()
            }.map { listOf(it) }
        return InlineKeyboardMarkup.builder().keyboard(rowList).build()
    }
}