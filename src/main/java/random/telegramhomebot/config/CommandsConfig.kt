package random.telegramhomebot.config

import org.springframework.beans.factory.config.PropertiesFactoryBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.io.ClassPathResource

@Configuration
class CommandsConfig {
    @Bean(name = ["telegramCommands"])
    fun telegramCommands(): PropertiesFactoryBean {
        val bean = PropertiesFactoryBean()
        bean.setLocation(ClassPathResource("commands.properties"))
        return bean
    }
}