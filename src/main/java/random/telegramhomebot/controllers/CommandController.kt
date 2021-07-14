package random.telegramhomebot.controllers

import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import random.telegramhomebot.AppConstants
import random.telegramhomebot.model.TelegramCommand
import random.telegramhomebot.repository.TelegramCommandRepository
import java.util.*

@Controller
@RequestMapping(AppConstants.Commands.COMMANDS_MAPPING)
class CommandController(
    private val telegramCommandRepository: TelegramCommandRepository
) {
    @RequestMapping
    fun getAllCommands(model: Model): String {
        model.addAttribute(AppConstants.Commands.COMMANDS_MODEL_ATTR, telegramCommandRepository.findAll())
        return AppConstants.Commands.COMMANDS_VIEW
    }

    @RequestMapping(path = [AppConstants.Commands.EDIT_COMMAND_MAPPING, AppConstants.Commands.EDIT_COMMAND_BY_ID_MAPPING])
    fun editCommandById(
        model: Model, @PathVariable(AppConstants.Commands.COMMAND_ID_PATH_VAR) id: Optional<UUID>
    ): String {
        model.addAttribute(
            AppConstants.Commands.COMMAND_MODEL_ATTR, Optional.ofNullable(id)
                .filter { obj -> obj.isPresent }
                .flatMap { opId -> telegramCommandRepository.findById(opId.get()) }
                .orElseGet { TelegramCommand() })
        return AppConstants.Commands.ADD_EDIT_COMMAND_VIEW
    }

    @RequestMapping(path = [AppConstants.Commands.DELETE_COMMAND_MAPPING])
    fun deleteCommandById(
        @PathVariable(AppConstants.Commands.COMMAND_ID_PATH_VAR) id: UUID
    ): String {
        telegramCommandRepository.deleteById(id)
        return AppConstants.Commands.REDIRECT_COMMANDS
    }

    @PostMapping(path = [AppConstants.Commands.SAVE_COMMAND_MAPPING])
    fun createOrUpdateCommand(command: TelegramCommand): String {
        telegramCommandRepository.save(command)
        return AppConstants.Commands.REDIRECT_COMMANDS
    }
}
