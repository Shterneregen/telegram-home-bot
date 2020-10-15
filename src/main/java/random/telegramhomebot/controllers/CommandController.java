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

@Controller
@RequestMapping("/")
public class CommandController {

	private static final String COMMANDS = "commands";
	private static final String COMMAND = "command";

	@Resource
	private TelegramCommandRepository telegramCommandRepository;

	@RequestMapping
	public String getAllCommands(Model model) {
		model.addAttribute(COMMANDS, telegramCommandRepository.findAll());
		return "commands";
	}

	@RequestMapping(path = {"/edit", "/edit/{id}"})
	public String editCommandById(Model model, @PathVariable("id") Optional<UUID> id) {
		model.addAttribute(COMMAND, Optional.ofNullable(id)
				.filter(Optional::isPresent)
				.flatMap(macOp -> telegramCommandRepository.findById(id.get()))
				.orElseGet(TelegramCommand::new));
		return "add-edit-command";
	}

	@RequestMapping(path = "/delete/{id}")
	public String deleteCommandById(@PathVariable("id") UUID id) {
		telegramCommandRepository.deleteById(id);
		return "redirect:/";
	}

	@PostMapping(path = "/createCommand")
	public String createOrUpdateCommand(TelegramCommand command) {
		telegramCommandRepository.save(command);
		return "redirect:/";
	}
}
