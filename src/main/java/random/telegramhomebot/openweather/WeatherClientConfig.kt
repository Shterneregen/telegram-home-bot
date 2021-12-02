package random.telegramhomebot.openweather

import io.netty.handler.logging.LogLevel
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.web.reactive.function.client.WebClient
import reactor.netty.http.client.HttpClient
import reactor.netty.transport.logging.AdvancedByteBufFormat

@Configuration
class WeatherClientConfig(private val properties: OpenWeatherProperties) {

    @Bean
    fun weatherWebClient(): WebClient = WebClient.builder().baseUrl(properties.baseUrl).clientConnector(
        ReactorClientHttpConnector(
            HttpClient.create()
                .wiretap("reactor.netty.client.HttpClient", LogLevel.DEBUG, AdvancedByteBufFormat.TEXTUAL)
        )
    ).build()

    @Bean
    fun weatherClient(): WeatherClient = WeatherClient(weatherWebClient(), properties)
}
