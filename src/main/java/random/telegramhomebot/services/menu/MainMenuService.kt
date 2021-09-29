package random.telegramhomebot.services.menu

import org.springframework.stereotype.Service
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton
import random.telegramhomebot.AppConstants.BotCommands.LAST_ACTIVITY
import random.telegramhomebot.AppConstants.BotCommands.REACHABLE_HOSTS_COMMAND
import random.telegramhomebot.AppConstants.BotCommands.REFRESH
import random.telegramhomebot.AppConstants.BotCommands.SHOW_ALL_COMMANDS
import random.telegramhomebot.services.CommandService
import random.telegramhomebot.services.HostService
import random.telegramhomebot.services.MessageFormatService
import random.telegramhomebot.services.StateChangeService
import random.telegramhomebot.services.menu.dto.Menu
import random.telegramhomebot.telegram.Icon

@Service
class MainMenuService(
    private val hostService: HostService,
    private val messageFormatService: MessageFormatService,
    private val commandService: CommandService,
    private val stateChangeService: StateChangeService,
) : MenuService {
    override val menuCommand = "/menu"
    override val menuText = "Main Menu"

    override fun getMenuMap(): Map<String, Menu> = mapOf(
        REACHABLE_HOSTS_COMMAND to Menu(Icon.DESKTOP_COMPUTER.get())
        { messageFormatService.getHostsState(hostService.getReachableHosts()).ifBlank { "No hosts" } },
        SHOW_ALL_COMMANDS to Menu(Icon.HAMMER.get())
        { "Available commands:\n" + commandService.getAllEnabledCommandsAsString().ifBlank { "No commands" } },
        LAST_ACTIVITY to Menu(Icon.SCROLL.get())
        { "History:\n" + hostService.getLastHostTimeLogsAsString(20).ifBlank { "No activity" } },
        REFRESH to Menu(Icon.REFRESH.get())
        {
            stateChangeService.checkState()
            "Host scan started"
        }
    )

    override fun getMenuInlineKeyboardMarkup(): InlineKeyboardMarkup {
        val buttons = getMenuMap().entries.map { (command, menu) ->
            InlineKeyboardButton.builder()
                .text(menu.buttonText)
                .callbackData(command).build()
        }
        return InlineKeyboardMarkup.builder().keyboard(listOf(buttons)).build()
    }
}
