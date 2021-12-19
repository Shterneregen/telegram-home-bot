package random.telegramhomebot.openweather

import io.netty.handler.logging.LogLevel
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.web.reactive.function.client.WebClient
import reactor.netty.http.client.HttpClient
import reactor.netty.transport.logging.AdvancedByteBufFormat
import java.time.Duration

@Configuration
@ConditionalOnProperty(prefix = "openweather", value = ["enabled"], havingValue = "true")
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
