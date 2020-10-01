package random.telegramhomebot;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import random.telegramhomebot.repository.HostRepository;
import random.telegramhomebot.telegram.HomeBot;
import random.telegramhomebot.utils.MessageUtil;

import javax.annotation.Resource;

@Component
public class Loader implements CommandLineRunner {

	private static final String STORED_HOSTS = "Stored Hosts";

	@Resource
	private HomeBot homeBot;
	@Resource
	private HostRepository hostRepository;
	@Resource
	private MessageUtil messageUtil;

	@Override
	public void run(String... args) {
		sendBootstrapMessage();
	}

	private void sendBootstrapMessage() {
		homeBot.sendMessage("Chatbot started!\n\n" + messageUtil.formHostsListTable(hostRepository.findAll(), STORED_HOSTS));
	}
}
