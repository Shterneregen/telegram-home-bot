package random.telegramhomebot.controllers

import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import random.telegramhomebot.AppConstants.Commands.*
import random.telegramhomebot.model.TelegramCommand
import random.telegramhomebot.repository.TelegramCommandRepository
import java.util.*

@Controller
@RequestMapping(COMMANDS_MAPPING)
class CommandController(
    private val telegramCommandRepository: TelegramCommandRepository
) {
    @RequestMapping
    fun getAllCommands(model: Model): String {
        model.addAttribute(COMMANDS_MODEL_ATTR, telegramCommandRepository.findAll())
        return COMMANDS_VIEW
    }

    @RequestMapping(path = [EDIT_COMMAND_MAPPING, EDIT_COMMAND_BY_ID_MAPPING])
    fun editCommandById(model: Model, @PathVariable(COMMAND_ID_PATH_VAR) id: Optional<UUID>): String {
        model.addAttribute(COMMAND_MODEL_ATTR, Optional.ofNullable(id)
            .filter { obj -> obj.isPresent }
            .flatMap { opId -> telegramCommandRepository.findById(opId.get()) }
            .orElseGet { TelegramCommand() })
        return ADD_EDIT_COMMAND_VIEW
    }

    @RequestMapping(path = [DELETE_COMMAND_MAPPING])
    fun deleteCommandById(@PathVariable(COMMAND_ID_PATH_VAR) id: UUID): String {
        telegramCommandRepository.deleteById(id)
        return REDIRECT_COMMANDS
    }

    @PostMapping(path = [SAVE_COMMAND_MAPPING])
    fun createOrUpdateCommand(command: TelegramCommand): String {
        telegramCommandRepository.save(command)
        return REDIRECT_COMMANDS
    }
}
