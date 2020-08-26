package random.telegramnetworkclientnotifier;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class Schedule {

	@Autowired
	private TelegramNotifier telegramNotifier;

	@Scheduled(fixedRate = 10000)
	public void scanNetwork() {
		telegramNotifier.sendMessage("Hello!");
	}
}
