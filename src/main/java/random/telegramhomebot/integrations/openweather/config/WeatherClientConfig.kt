package random.telegramhomebot.integrations.openweather.config

import io.netty.handler.logging.LogLevel
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.web.reactive.function.client.WebClient
import random.telegramhomebot.integrations.openweather.WeatherService
import reactor.netty.http.client.HttpClient
import reactor.netty.transport.logging.AdvancedByteBufFormat
import java.time.Duration

@Configuration
@ConditionalOnProperty(name = ["integrations.openweather.enabled"], havingValue = "true")
class WeatherClientConfig(private val properties: OpenWeatherProperties) {

    @Bean
    fun weatherWebClient(): WebClient = WebClient.builder().baseUrl(properties.baseUrl)
        .clientConnector(
            ReactorClientHttpConnector(
                HttpClient.create()
                    .responseTimeout(Duration.ofSeconds(1))
                    .wiretap("reactor.netty.client.HttpClient", LogLevel.DEBUG, AdvancedByteBufFormat.TEXTUAL)
            )
        ).build()

    @Bean
    fun weatherService(): WeatherService = WeatherService(weatherWebClient(), properties)
}
