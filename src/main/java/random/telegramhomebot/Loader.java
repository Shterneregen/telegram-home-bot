package random.telegramhomebot;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import random.telegramhomebot.model.Host;
import random.telegramhomebot.repository.HostRepository;
import random.telegramhomebot.telegram.HomeBot;

import javax.annotation.Resource;
import java.util.List;

@Component
public class Loader implements CommandLineRunner {

	private static final String STORED_HOSTS = "Stored Hosts";

	@Resource
	private HomeBot homeBot;
	@Resource
	private HostRepository hostRepository;

	@Override
	public void run(String... args) {
		sendBootstrapMessage();
	}

	private void sendBootstrapMessage() {
		List<Host> hosts = hostRepository.findAll();
		homeBot.sendMessage("Chatbot started!\n\n" + homeBot.formHostsListTable(hosts, STORED_HOSTS));
	}
}
