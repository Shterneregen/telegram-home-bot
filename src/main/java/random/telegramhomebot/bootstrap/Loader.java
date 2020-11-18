package random.telegramhomebot.bootstrap;

import org.springframework.boot.CommandLineRunner;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import random.telegramhomebot.config.Profiles;
import random.telegramhomebot.repository.HostRepository;
import random.telegramhomebot.telegram.Bot;
import random.telegramhomebot.utils.MessageConfigurer;
import random.telegramhomebot.utils.MessageUtil;

import javax.annotation.Resource;
import java.util.List;

@Component
public class Loader implements CommandLineRunner {

	@Resource
	private Bot bot;
	@Resource
	private HostRepository hostRepository;
	@Resource
	private MessageUtil messageUtil;
	@Resource
	private Environment environment;
	@Resource
	private MessageConfigurer messageConfigurer;

	@Override
	public void run(String... args) {
		sendBootstrapMessage();
	}

	private void sendBootstrapMessage() {
		StringBuilder messageBuilder = new StringBuilder(messageConfigurer.getMessage("chatbot.started"));

		if (List.of(environment.getActiveProfiles()).contains(Profiles.NETWORK_MONITOR)) {
			String hostsState = messageUtil.getHostsState(hostRepository.findAll());
			messageBuilder.append("\n\n").append(hostsState);
		}

		bot.sendMessage(messageBuilder.toString());
	}
}
