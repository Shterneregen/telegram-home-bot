package random.telegramhomebot.openweather

import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.util.UriBuilder
import reactor.core.publisher.Mono
import java.util.*

class WeatherService(private val weatherWebClient: WebClient, private val properties: OpenWeatherProperties) {

    fun getWeather(
        cityId: String? = null,
        lat: String? = null,
        lon: String? = null
    ): Mono<WeatherResponse> {
        return weatherWebClient.get()
            .uri { uriBuilder: UriBuilder ->
                uriBuilder.path("/weather")
                    .queryParamIfPresent("id", Optional.ofNullable(cityId))
                    .queryParamIfPresent("lat", Optional.ofNullable(lat))
                    .queryParamIfPresent("lon", Optional.ofNullable(lon))
                    .queryParamIfPresent("appid", Optional.ofNullable(properties.appid))
                    .queryParamIfPresent("units", Optional.ofNullable(Units.METRIC.code))
                    .build()
            }
            .retrieve()
            .bodyToMono(WeatherResponse::class.java)
    }
}
