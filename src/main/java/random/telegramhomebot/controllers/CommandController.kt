package random.telegramhomebot.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import random.telegramhomebot.model.TelegramCommand;
import random.telegramhomebot.repository.TelegramCommandRepository;

import javax.annotation.Resource;
import java.util.Optional;
import java.util.UUID;

import static random.telegramhomebot.AppConstants.Commands.ADD_EDIT_COMMAND_VIEW;
import static random.telegramhomebot.AppConstants.Commands.COMMANDS_MAPPING;
import static random.telegramhomebot.AppConstants.Commands.COMMANDS_MODEL_ATTR;
import static random.telegramhomebot.AppConstants.Commands.COMMANDS_VIEW;
import static random.telegramhomebot.AppConstants.Commands.COMMAND_ID_PATH_VAR;
import static random.telegramhomebot.AppConstants.Commands.COMMAND_MODEL_ATTR;
import static random.telegramhomebot.AppConstants.Commands.DELETE_COMMAND_MAPPING;
import static random.telegramhomebot.AppConstants.Commands.EDIT_COMMAND_BY_ID_MAPPING;
import static random.telegramhomebot.AppConstants.Commands.EDIT_COMMAND_MAPPING;
import static random.telegramhomebot.AppConstants.Commands.REDIRECT_COMMANDS;
import static random.telegramhomebot.AppConstants.Commands.SAVE_COMMAND_MAPPING;

@Controller
@RequestMapping(COMMANDS_MAPPING)
public class CommandController {

    @Resource
    private TelegramCommandRepository telegramCommandRepository;

    @RequestMapping
    public String getAllCommands(Model model) {
        model.addAttribute(COMMANDS_MODEL_ATTR, telegramCommandRepository.findAll());
        return COMMANDS_VIEW;
    }

    @RequestMapping(path = {EDIT_COMMAND_MAPPING, EDIT_COMMAND_BY_ID_MAPPING})
    public String editCommandById(Model model, @PathVariable(COMMAND_ID_PATH_VAR) Optional<UUID> id) {
        model.addAttribute(COMMAND_MODEL_ATTR, Optional.ofNullable(id)
                .filter(Optional::isPresent)
                .flatMap(macOp -> telegramCommandRepository.findById(id.get()))
                .orElseGet(() -> new TelegramCommand()));
        return ADD_EDIT_COMMAND_VIEW;
    }

    @RequestMapping(path = DELETE_COMMAND_MAPPING)
    public String deleteCommandById(@PathVariable(COMMAND_ID_PATH_VAR) UUID id) {
        telegramCommandRepository.deleteById(id);
        return REDIRECT_COMMANDS;
    }

    @PostMapping(path = SAVE_COMMAND_MAPPING)
    public String createOrUpdateCommand(TelegramCommand command) {
        telegramCommandRepository.save(command);
        return REDIRECT_COMMANDS;
    }
}
