package random.telegramhomebot.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import random.telegramhomebot.model.TelegramCommand;
import random.telegramhomebot.repository.TelegramCommandRepository;

import javax.annotation.Resource;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Controller
@RequestMapping("/")
public class CommandController {

	private static final String COMMAND = "command";

	@Resource
	private TelegramCommandRepository telegramCommandRepository;

	@RequestMapping
	public String getAllCommands(Model model) {
		List<TelegramCommand> commands = telegramCommandRepository.findAll();
		model.addAttribute("commands", commands);
		return "commands";
	}

	@RequestMapping(path = {"/edit", "/edit/{id}"})
	public String editEmployeeById(Model model, @PathVariable("id") Optional<UUID> id) {
		if (id.isPresent()) {
			Optional<TelegramCommand> commandOp = telegramCommandRepository.findById(id.get());
			if (commandOp.isPresent()) {
				model.addAttribute(COMMAND, commandOp.get());
			} else {
				model.addAttribute(COMMAND, new TelegramCommand());
			}
		} else {
			model.addAttribute(COMMAND, new TelegramCommand());
		}
		return "add-edit-command";
	}

	@RequestMapping(path = "/delete/{id}")
	public String deleteEmployeeById(Model model, @PathVariable("id") UUID id) {
		telegramCommandRepository.deleteById(id);
		return "redirect:/";
	}

	@PostMapping(path = "/createCommand")
	public String createOrUpdateEmployee(TelegramCommand command) {
		telegramCommandRepository.save(command);
		return "redirect:/";
	}
}
