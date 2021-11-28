package random.telegramhomebot.services.commands

import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.actuate.health.Health
import org.springframework.boot.actuate.health.HealthIndicator
import org.springframework.stereotype.Service
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow
import random.telegramhomebot.db.model.Command
import random.telegramhomebot.db.model.TelegramCommand
import random.telegramhomebot.db.repository.TelegramCommandRepository
import random.telegramhomebot.utils.logger

@Service
class CommandService(
    private val telegramCommandRepository: TelegramCommandRepository,
    private val commandRunnerService: CommandRunnerService
) : HealthIndicator {
    val log = logger()

    @Value("\${buttons.in.row}")
    private val buttonsInRow = 0

    fun getAllEnabledCommands(): List<TelegramCommand> = telegramCommandRepository.findAllEnabled()

    fun getEnabledCommand(commandAlias: String?): TelegramCommand? =
        telegramCommandRepository.findByCommandAliasAndEnabled(commandAlias, true)

    fun getAllEnabledCommandsAsString() =
        getAllEnabledCommands().joinToString(separator = "\n") { "${it.commandAlias} = ${it.command}" }

    fun executeCommandOnMachine(command: String?): String? {
        val telegramCommand = getEnabledCommand(command) ?: return null
        return commandRunnerService.runCommand(telegramCommand.command)
    }

    fun createReplyMarkupCommandButtons(): ReplyKeyboardMarkup? {
        val enabledCommands = getAllEnabledCommands()
        log.debug("Enabled commands: {}", enabledCommands)
        if (enabledCommands.isEmpty()) {
            return null
        }
        return ReplyKeyboardMarkup.builder()
            .selective(true)
            .resizeKeyboard(true)
            .oneTimeKeyboard(false)
            .keyboard(getKeyboardRows(enabledCommands, buttonsInRow))
            .build()
    }

    private fun getKeyboardRows(commands: List<Command>, buttonsInRow: Int): List<KeyboardRow> {
        val keyboardRowList: MutableList<KeyboardRow> = ArrayList()
        var keyboardRow = KeyboardRow()

        for (i in commands.indices) {
            val newRow = i % buttonsInRow == 0
            if (newRow) {
                keyboardRow = KeyboardRow()
            }
            keyboardRow.add(KeyboardButton(commands[i].getButtonName()))
            val lastInRow = (i + 1) % buttonsInRow == 0 || i == commands.size - 1
            if (lastInRow) {
                keyboardRowList.add(keyboardRow)
            }
        }
        return keyboardRowList
    }

    override fun health(): Health =
        if (telegramCommandRepository.findAll().size > 0) Health.up().build() else Health.down().build()
}
