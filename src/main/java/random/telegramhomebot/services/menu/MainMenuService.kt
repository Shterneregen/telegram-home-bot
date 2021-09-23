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
import random.telegramhomebot.services.MessageService
import random.telegramhomebot.services.StateChangeService
import random.telegramhomebot.telegram.Icon

@Service
class MainMenuService(
    private val hostService: HostService,
    private val messageFormatService: MessageFormatService,
    private val commandService: CommandService,
    private val messageService: MessageService,
    private val stateChangeService: StateChangeService,
) {

    fun getMainMenuMap(): Map<String, Menu> {
        return mapOf(
            REACHABLE_HOSTS_COMMAND to Menu(messageService.getMessage("btn.hosts"))
            { messageFormatService.getHostsState(hostService.getReachableHosts()).ifBlank { "No hosts" } },
            SHOW_ALL_COMMANDS to Menu(messageService.getMessage("btn.commands"))
            { commandService.getAllEnabledCommandsAsString().ifBlank { "No commands" } },
            LAST_ACTIVITY to Menu(messageService.getMessage("btn.activity"))
            { hostService.getLastHostTimeLogsAsString(20).ifBlank { "No activity" } },
            REFRESH to Menu(Icon.REFRESH.get())
            {
                stateChangeService.checkState()
                "Host scan started"
            }
        )
    }

    fun getMainMenuInlineKeyboardMarkup(): InlineKeyboardMarkup {
        val buttons = getMainMenuMap().entries
            .map { (key, menu) ->
                InlineKeyboardButton.builder()
                    .text(menu.buttonText)
                    .callbackData(key).build()
            }
        return InlineKeyboardMarkup.builder().keyboard(listOf(buttons)).build()
    }
}
