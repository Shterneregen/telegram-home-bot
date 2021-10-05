package random.telegramhomebot.controllers

import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import random.telegramhomebot.AppConstants.Commands.*
import random.telegramhomebot.AppConstants.Redirects.ERROR_404_REDIRECT
import random.telegramhomebot.db.model.TelegramCommand
import random.telegramhomebot.db.repository.TelegramCommandRepository
import java.util.*

@Controller
@RequestMapping(COMMANDS_MAPPING)
class CommandController(private val telegramCommandRepository: TelegramCommandRepository) {

    @RequestMapping
    fun getAllCommands(model: Model): String {
        model.addAttribute(COMMANDS_MODEL_ATTR, telegramCommandRepository.findAll())
        return COMMANDS_VIEW
    }

    @RequestMapping(path = [EDIT_COMMAND_MAPPING, EDIT_COMMAND_BY_ID_MAPPING])
    fun editCommandById(model: Model, @PathVariable(COMMAND_ID_PATH_VAR) id: UUID?): String {
        val command =
            if (id == null) TelegramCommand()
            else id.let { telegramCommandRepository.findById(it).orElse(null) } ?: return ERROR_404_REDIRECT

        model.addAttribute(COMMAND_MODEL_ATTR, command)
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
