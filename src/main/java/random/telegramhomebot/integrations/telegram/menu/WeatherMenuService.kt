package random.telegramhomebot.integrations.telegram.menu

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Service
import random.telegramhomebot.integrations.openweather.WeatherService
import random.telegramhomebot.integrations.openweather.db.WeatherItemRepository
import random.telegramhomebot.integrations.telegram.menu.dto.Menu

@Service
@ConditionalOnProperty(name = ["integrations.openweather.enabled"], havingValue = "true")
class WeatherMenuService(
    private val weatherService: WeatherService,
    private val weatherItemRepository: WeatherItemRepository
) : MenuService {
    override val menuCommand = "/weather"
    override val menuText = "Weather"

    override fun getMenuMap(): Map<String, Menu> {
        val allWeatherItems = weatherItemRepository.findAll()

        val mapByCode = allWeatherItems.filter { it.cityId != null && it.cityId!!.isNotBlank() }.associate {
            val cityId = it.cityId
            val cityName = it.cityName
            "/$cityId" to Menu("Get weather for '$cityName'") {
                weatherService.getWeather(cityId = cityId)
                    .map { weather -> "Weather for '$cityName' is [${weather?.main?.temp}]" }
                    .onErrorReturn("Weather service unavailable for '$cityName'")
                    .defaultIfEmpty("Weather data not found for '$cityName'")
                    .block()!!
            }
        }

        val mapByCoordinates = allWeatherItems.filter { it.lat != null && it.lon != null }.associate {
            val cityName = it.cityName
            "/$cityName" to Menu("Get weather for '$cityName'") {
                weatherService.getWeather(lat = it.lat, lon = it.lon)
                    .map { weather -> "Weather for [${weather?.coord}] is [${weather?.main?.temp}]" }
                    .onErrorReturn("Weather service unavailable")
                    .defaultIfEmpty("Weather data not found")
                    .block()!!
            }
        }
        return mapByCode + mapByCoordinates
    }

    override fun getMenuInlineKeyboardMarkup() = getDefaultVerticalMenuInlineKeyboardMarkup()
}
