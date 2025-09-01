package random.telegramhomebot.integrations.openweather

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.util.UriBuilder
import random.telegramhomebot.integrations.openweather.config.OpenWeatherProperties
import reactor.core.publisher.Mono
import java.util.Optional.ofNullable

@ConditionalOnProperty(name = ["integrations.openweather.enabled"], havingValue = "true")
class WeatherService(
    private val weatherWebClient: WebClient,
    private val properties: OpenWeatherProperties
) {
    fun getWeather(
        cityId: String? = null,
        lat: String? = null,
        lon: String? = null
    ): Mono<WeatherResponse> {
        return weatherWebClient.get()
            .uri { uriBuilder: UriBuilder ->
                uriBuilder.path("/weather")
                    .queryParamIfPresent("id", ofNullable(cityId))
                    .queryParamIfPresent("lat", ofNullable(lat))
                    .queryParamIfPresent("lon", ofNullable(lon))
                    .queryParamIfPresent("appid", ofNullable(properties.appid))
                    .queryParamIfPresent("units", ofNullable(Units.METRIC.code))
                    .build()
            }
            .retrieve()
            .bodyToMono(WeatherResponse::class.java)
    }
}
