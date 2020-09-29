package random.telegramhomebot;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ImportResource;
import org.telegram.telegrambots.ApiContextInitializer;

@SpringBootApplication
@ImportResource("classpath:commands.xml")
public class TelegramHomeBotApplication {

	@Bean
	ObjectMapper objectMapper() {
		return new ObjectMapper();
	}

	public static void main(String[] args) {
		ApiContextInitializer.init();
		SpringApplication.run(TelegramHomeBotApplication.class, args);
	}

}
