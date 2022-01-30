package random.telegramhomebot.controllers

import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import random.telegramhomebot.const.AppConstants.ADD_EDIT_COMMAND_VIEW
import random.telegramhomebot.const.AppConstants.COMMANDS_MAPPING
import random.telegramhomebot.const.AppConstants.COMMANDS_MODEL_ATTR
import random.telegramhomebot.const.AppConstants.COMMANDS_VIEW
import random.telegramhomebot.const.AppConstants.COMMAND_ID_PATH_VAR
import random.telegramhomebot.const.AppConstants.COMMAND_MODEL_ATTR
import random.telegramhomebot.const.AppConstants.DELETE_COMMAND_MAPPING
import random.telegramhomebot.const.AppConstants.EDIT_COMMAND_BY_ID_MAPPING
import random.telegramhomebot.const.AppConstants.EDIT_COMMAND_MAPPING
import random.telegramhomebot.const.AppConstants.ERROR_404_REDIRECT
import random.telegramhomebot.const.AppConstants.REDIRECT_COMMANDS
import random.telegramhomebot.const.AppConstants.SAVE_COMMAND_MAPPING
import random.telegramhomebot.db.model.TelegramCommand
import random.telegramhomebot.db.repository.TelegramCommandRepository
import reactor.core.publisher.Mono
import java.util.UUID

@Controller
@RequestMapping(COMMANDS_MAPPING)
class CommandController(private val telegramCommandRepository: TelegramCommandRepository) {

    @RequestMapping
    fun getAllCommands(model: Model): Mono<String> {
        return telegramCommandRepository.findAll().collectList()
            .flatMap { commands ->
                model.addAttribute(COMMANDS_MODEL_ATTR, commands)
                Mono.just(COMMANDS_VIEW)
            }
//        model.addAttribute(COMMANDS_MODEL_ATTR, telegramCommandRepository.findAll().collectList().block(ofSeconds(10)))
//        return COMMANDS_VIEW
    }

    @RequestMapping(path = [EDIT_COMMAND_MAPPING, EDIT_COMMAND_BY_ID_MAPPING])
    fun editCommandById(model: Model, @PathVariable(COMMAND_ID_PATH_VAR) id: UUID?): Mono<String> {
        return (id?.let { telegramCommandRepository.findById(it) } ?: Mono.just(TelegramCommand()))
            .flatMap { command ->
                model.addAttribute(COMMAND_MODEL_ATTR, command)
                Mono.just(ADD_EDIT_COMMAND_VIEW)
            }.switchIfEmpty(Mono.just(ERROR_404_REDIRECT))
    }

    @RequestMapping(path = [DELETE_COMMAND_MAPPING])
    fun deleteCommandById(@PathVariable(COMMAND_ID_PATH_VAR) id: UUID): Mono<String> {
        return telegramCommandRepository.deleteById(id).thenReturn(REDIRECT_COMMANDS)
    }

    @PostMapping(path = [SAVE_COMMAND_MAPPING])
    fun createOrUpdateCommand(command: TelegramCommand): Mono<String> {
        return telegramCommandRepository.save(command).thenReturn(REDIRECT_COMMANDS)
    }
}
