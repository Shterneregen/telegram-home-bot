package random.telegramnetworkclientnotifier;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.telegram.telegrambots.ApiContextInitializer;

@SpringBootApplication
public class TelegramNetworkClientNotifierApplication {

	public static void main(String[] args) {
		ApiContextInitializer.init();
		SpringApplication.run(TelegramNetworkClientNotifierApplication.class, args);
	}

}
