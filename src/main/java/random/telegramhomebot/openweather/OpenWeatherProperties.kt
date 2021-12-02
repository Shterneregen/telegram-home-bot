package random.telegramhomebot.openweather

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@ConfigurationProperties(prefix = "openweather")
class OpenWeatherProperties {
    lateinit var baseUrl: String
    lateinit var appid: String
}
