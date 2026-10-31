package random.telegramhomebot.services.commands

import org.springframework.boot.actuate.health.Health
import org.springframework.boot.actuate.health.HealthIndicator
import org.springframework.stereotype.Service
import random.telegramhomebot.db.model.TelegramCommand
import random.telegramhomebot.db.repository.TelegramCommandRepository
import random.telegramhomebot.utils.logger

@Service
class CommandService(
    private val telegramCommandRepository: TelegramCommandRepository,
    private val commandRunnerService: CommandRunnerService,
) : HealthIndicator {
    val log = logger()

    fun getAllEnabledCommands(): List<TelegramCommand> = telegramCommandRepository.findAllEnabled()

    fun getEnabledCommand(commandAlias: String?): TelegramCommand? =
        telegramCommandRepository.findByCommandAliasAndEnabled(commandAlias, true)

    fun getAllEnabledCommandsAsString() = getAllEnabledCommands().joinToString(separator = "\n") { "${it.commandAlias} = ${it.command}" }

    fun executeCommandOnMachine(command: String?): String? {
        val telegramCommand = getEnabledCommand(command) ?: return null
        return commandRunnerService.runCommand(telegramCommand.command)
    }

    override fun health(): Health = if (telegramCommandRepository.findAll().size > 0) Health.up().build() else Health.down().build()
}
