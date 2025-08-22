package random.telegramhomebot.integrations.telegram.menu

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Service
import random.telegramhomebot.const.AppConstants.DATE_TIME_FORMATTER
import random.telegramhomebot.integrations.telegram.menu.dto.Menu
import random.telegramhomebot.services.WakeOnLanService
import random.telegramhomebot.monitor.hosts.HostService
import java.time.LocalDateTime.now

@Service
@ConditionalOnProperty(name = ["integrations.telegram.enabled"], havingValue = "true")
class WakeOnLanMenuService(
    private val hostService: HostService,
    private val wakeOnLanService: WakeOnLanService
) : MenuService {
    override val menuCommand = "/wol"
    override val menuText = "Wake On Lan hosts"

    override fun getMenuMap(): Map<String, Menu> {
        return hostService.getWakeOnLanEnableHosts()
            .associate {
                "$menuCommand${it.id}" to Menu("Wake-up '${it.deviceName ?: it.mac!!}'") {
                    wakeOnLanService.wakeOnLan(it.mac)
                    "Wake-up request sent to '${it.deviceName}' [${now().format(DATE_TIME_FORMATTER)}]"
                }
            }
    }

    override fun getMenuInlineKeyboardMarkup() = getDefaultVerticalMenuInlineKeyboardMarkup()
}
