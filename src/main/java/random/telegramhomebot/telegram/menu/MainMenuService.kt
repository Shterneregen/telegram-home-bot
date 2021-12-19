package random.telegramhomebot.telegram.menu

import org.springframework.stereotype.Service
import random.telegramhomebot.const.AppConstants.LAST_ACTIVITY
import random.telegramhomebot.const.AppConstants.REACHABLE_HOSTS_COMMAND
import random.telegramhomebot.const.AppConstants.REFRESH
import random.telegramhomebot.const.AppConstants.SHOW_ALL_COMMANDS
import random.telegramhomebot.events.scan.ScanHostsEventPublisher
import random.telegramhomebot.services.commands.CommandService
import random.telegramhomebot.services.hosts.HostService
import random.telegramhomebot.services.messages.MessageFormatService
import random.telegramhomebot.telegram.Icon
import random.telegramhomebot.telegram.menu.dto.Menu

@Service
class MainMenuService(
    private val hostService: HostService,
    private val messageFormatService: MessageFormatService,
    private val commandService: CommandService,
    private val scanHostsEventPublisher: ScanHostsEventPublisher
) : MenuService {
    override val menuCommand = "/menu"
    override val menuText = "Main Menu"

    override fun getMenuMap(): Map<String, Menu> = mapOf(
        REACHABLE_HOSTS_COMMAND to Menu(Icon.DESKTOP_COMPUTER.get()) {
            messageFormatService.getHostsState(hostService.getReachableHosts()).ifBlank { "No hosts" }
        },
        SHOW_ALL_COMMANDS to Menu(Icon.HAMMER.get()) {
            "Available commands:\n" + commandService.getAllEnabledCommandsAsString().ifBlank { "No commands" }
        },
        LAST_ACTIVITY to Menu(Icon.SCROLL.get()) {
            "History:\n" + hostService.getLastHostTimeLogsAsString(20).ifBlank { "No activity" }
        },
        REFRESH to Menu(Icon.REFRESH.get()) {
            scanHostsEventPublisher.publishEvent()
            "Host scan started"
        }
    )

    override fun getMenuInlineKeyboardMarkup() = getDefaultHorizontalMenuInlineKeyboardMarkup()
}
