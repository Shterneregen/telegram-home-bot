package random.telegramhomebot.integrations.openweather.config

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

@Component
@ConditionalOnProperty(name = ["integrations.openweather.enabled"], havingValue = "true")
@ConfigurationProperties(prefix = "integrations.openweather")
class OpenWeatherProperties {
    lateinit var baseUrl: String
    lateinit var appid: String
}
