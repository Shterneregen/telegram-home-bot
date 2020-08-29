package random.telegramnetworkclientnotifier;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Service
public class Scheduler {

	private final CommandRunner commandRunner;
	private final TelegramNotifier telegramNotifier;

	private List<String> previousArp;

	@Scheduled(fixedRate = 300000)
	public void scanNetwork() {
		List<String> arp = commandRunner.runCommand("arp -a", "utf-8");
		log.debug(String.valueOf(arp));
		if (!arp.equals(previousArp)) {
			log.info("State changed!");
			log.info(String.valueOf(arp));
			telegramNotifier.sendMessageToPrivateGroup("State changed!");
		}
		previousArp = arp;
	}
}
