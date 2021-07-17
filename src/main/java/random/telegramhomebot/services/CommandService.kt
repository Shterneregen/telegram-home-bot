package random.telegramhomebot.services

import org.apache.commons.collections4.CollectionUtils
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.actuate.health.Health
import org.springframework.boot.actuate.health.HealthIndicator
import org.springframework.stereotype.Service
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow
import random.telegramhomebot.model.Command
import random.telegramhomebot.model.TelegramCommand
import random.telegramhomebot.repository.TelegramCommandRepository
import java.util.function.Consumer
import java.util.stream.Collectors

@Service
class CommandService(
    private val telegramCommandRepository: TelegramCommandRepository,
    private val commandRunnerService: CommandRunnerService
) : HealthIndicator {
    @Value("\${buttons.in.row}")
    private val buttonsInRow = 0

    fun getAllEnabledCommands(): List<TelegramCommand> = telegramCommandRepository.findAllEnabled()

    fun getEnabledCommand(commandAlias: String?): TelegramCommand? =
        telegramCommandRepository.findByCommandAliasAndEnabled(commandAlias, true)

    fun getAllEnabledCommandsAsString(): String = getAllEnabledCommands().stream()
        .map { command: TelegramCommand? -> "${command!!.commandAlias} = ${command.command}" }
        .collect(Collectors.joining("\n"))

    fun executeCommandOnMachine(command: String?): String? {
        val telegramCommand = getEnabledCommand(command)
        if (telegramCommand != null) {
            val commandOutput = commandRunnerService.runCommand(telegramCommand.command)
            commandOutput.forEach(Consumer { msg: String? -> log.debug(msg) })
            return java.lang.String.join("\n", commandOutput)
        }
        return null
    }

    fun setCommandButtons(sendMessage: SendMessage) {
        val enabledCommands = getAllEnabledCommands()
        log.debug("Enabled commands: {}", enabledCommands)
        if (CollectionUtils.isEmpty(enabledCommands)) {
            return
        }
        sendMessage.replyMarkup = ReplyKeyboardMarkup.builder()
            .selective(true)
            .resizeKeyboard(true)
            .oneTimeKeyboard(false)
            .keyboard(getKeyboardRows(enabledCommands, buttonsInRow))
            .build()
    }

    private fun getKeyboardRows(commands: List<Command>, buttonsInRow: Int): List<KeyboardRow> {
        val keyboardRowList: MutableList<KeyboardRow> = ArrayList()
        var keyboardRow = KeyboardRow()
        var i = 0
        var j = 0
        while (i < commands.size) {
            if (i % buttonsInRow == 0) {
                j++
                keyboardRow = KeyboardRow()
            }
            keyboardRow.add(KeyboardButton(commands[i].getButtonName()))
            if (buttonsInRow * j - 1 == i || i == commands.size - 1) {
                keyboardRowList.add(keyboardRow)
            }
            i++
        }
        return keyboardRowList
    }

    override fun health(): Health =
        if (telegramCommandRepository.findAll().size > 0) Health.up().build() else Health.down().build()

    companion object {
        @Suppress("JAVA_CLASS_ON_COMPANION")
        @JvmStatic
        private val log = LoggerFactory.getLogger(javaClass.enclosingClass)
    }
}