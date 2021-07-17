package random.telegramhomebot.bootstrap

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.CommandLineRunner
import org.springframework.stereotype.Component
import random.telegramhomebot.model.TelegramCommand
import random.telegramhomebot.repository.TelegramCommandRepository
import random.telegramhomebot.utils.logger
import java.util.stream.Collectors

@Component
class CommandsLoader(
    private val telegramCommandRepository: TelegramCommandRepository,
    @Qualifier("telegramCommands")
    private val telegramCommands: Map<String, String>
) : CommandLineRunner {
    val log = logger()

    override fun run(vararg args: String) {
        loadSampleCommands()
    }

    private fun loadSampleCommands() {
        val commandsCount = telegramCommandRepository.count()
        log.info("Stored commands count [{}]", commandsCount)
        if (commandsCount == 0L) {
            log.info("Loading sample commands...")
            telegramCommandRepository.saveAll(
                telegramCommands.entries.stream()
                    .map { (key, value) -> TelegramCommand(key, value, true) }
                    .collect(Collectors.toList()))
        }
    }
}
