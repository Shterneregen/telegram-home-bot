package random.telegramhomebot.openweather

import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.util.UriBuilder
import reactor.core.publisher.Mono
import java.util.*

class WeatherClient(private val weatherWebClient: WebClient, private val properties: OpenWeatherProperties) {

    fun getWeatherResponseForCityId(cityId: String): Mono<WeatherResponse> {
        return weatherWebClient.get().uri { uriBuilder: UriBuilder ->
            uriBuilder.path("/weather")
                .queryParamIfPresent("id", Optional.ofNullable(cityId))
                .queryParamIfPresent("appid", Optional.ofNullable(properties.appid))
                .queryParamIfPresent("units", Optional.ofNullable(Units.METRIC.code))
                .build()
        }
            .retrieve()
            .bodyToMono(WeatherResponse::class.java)
    }

    fun getWeatherResponseForCoordinates(lat: String, lon: String): Mono<WeatherResponse> {
        return weatherWebClient.get().uri { uriBuilder: UriBuilder ->
            uriBuilder.path("/weather")
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
