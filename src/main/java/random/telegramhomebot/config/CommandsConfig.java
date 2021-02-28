package random.telegramhomebot.config;

import org.springframework.beans.factory.config.PropertiesFactoryBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

@Configuration
public class CommandsConfig {
	@Bean(name = "telegramCommands")
	public PropertiesFactoryBean telegramCommands() {
		PropertiesFactoryBean bean = new PropertiesFactoryBean();
		bean.setLocation(new ClassPathResource("commands.properties"));
		return bean;
	}
}
