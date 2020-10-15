package random.telegramhomebot.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import random.telegramhomebot.model.Host;
import random.telegramhomebot.repository.HostRepository;

import javax.annotation.Resource;
import java.util.Comparator;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/hosts")
public class HostController {

	private static final String HOSTS = "hosts";
	private static final String HOST = "host";

	@Resource
	private HostRepository hostRepository;

	@RequestMapping
	public String getAllCommands(Model model) {
		model.addAttribute(HOSTS, hostRepository.findAll().stream()
				.sorted(Comparator.comparing(Host::getIp)).collect(Collectors.toList()));
		return HOSTS;
	}

	@RequestMapping(path = {"/edit", "/edit/{id}"})
	public String editHostById(Model model, @PathVariable("id") Optional<UUID> id) {
		model.addAttribute(HOST, Optional.ofNullable(id)
				.filter(Optional::isPresent)
				.flatMap(macOp -> hostRepository.findById(id.get()))
				.orElseGet(Host::new));
		return "add-edit-host";
	}

	@RequestMapping(path = "/delete/{id}")
	public String deleteHostById(@PathVariable("id") UUID id) {
		hostRepository.deleteById(id);
		return "redirect:/hosts";
	}

	@PostMapping(path = "/createHost")
	public String createOrUpdateHost(Host host) {
		hostRepository.save(host);
		return "redirect:/hosts";
	}
}
