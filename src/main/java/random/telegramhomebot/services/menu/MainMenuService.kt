package random.telegramhomebot.services.menu

import org.apache.commons.lang3.StringUtils
import org.springframework.stereotype.Service
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton
import random.telegramhomebot.AppConstants.BotCommands.*
import random.telegramhomebot.services.CommandService
import random.telegramhomebot.services.HostService
import random.telegramhomebot.services.MessageFormatService
import random.telegramhomebot.services.MessageService
import java.util.stream.Collectors

@Service
class MainMenuService(
    private val hostService: HostService,
    private val messageFormatService: MessageFormatService,
    private val commandService: CommandService,
    private val messageService: MessageService,
) {

    fun getMainMenuMap(): Map<String, Menu> {
        return mapOf(
            REACHABLE_HOSTS_COMMAND to Menu(messageService.getMessage("btn.hosts")) {
                val allHosts = messageFormatService.getHostsState(hostService.getReachableHosts())
                if (StringUtils.isNotBlank(allHosts)) allHosts else "No hosts"
            },
            SHOW_ALL_COMMANDS to Menu(messageService.getMessage("btn.commands")) {
                val allEnabledCommands = commandService.allEnabledCommandsAsString
                if (StringUtils.isNotBlank(allEnabledCommands)) allEnabledCommands else "No commands"
            },
            LAST_ACTIVITY to Menu(messageService.getMessage("btn.activity")) {
                val lastActivityStr = hostService.getLastHostTimeLogsAsString(20)
                if (StringUtils.isNotBlank(lastActivityStr)) lastActivityStr else "No activity"
            }
        )
    }

    fun getMainMenuInlineKeyboardMarkup(): InlineKeyboardMarkup {
        val buttons = getMainMenuMap().entries.stream()
            .map { (key, menu) ->
                InlineKeyboardButton.builder()
                    .text(menu.buttonText)
                    .callbackData(key).build()
            }.collect(Collectors.toList())
        return InlineKeyboardMarkup.builder().keyboard(listOf(buttons)).build()
    }
}
