package random.telegramhomebot.openweather

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

@Component
@ConditionalOnProperty(prefix = "openweather", value = ["enabled"], havingValue = "true")
@ConfigurationProperties(prefix = "openweather")
class OpenWeatherProperties {
    lateinit var baseUrl: String
    lateinit var appid: String
}
