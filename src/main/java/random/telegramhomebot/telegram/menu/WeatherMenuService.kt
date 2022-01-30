package random.telegramhomebot.telegram.menu

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Service
import random.telegramhomebot.openweather.WeatherService
import random.telegramhomebot.openweather.db.WeatherItemRepository
import random.telegramhomebot.telegram.menu.dto.Menu
import java.time.Duration.ofSeconds

@ConditionalOnProperty(prefix = "openweather", value = ["enabled"], havingValue = "true")
@Service
class WeatherMenuService(
    private val weatherService: WeatherService,
    private val weatherItemRepository: WeatherItemRepository
) : MenuService {
    override val menuCommand = "/weather"
    override val menuText = "Weather"

    override fun getMenuMap(): Map<String, Menu> {
        val allWeatherItems = weatherItemRepository.findAll().collectList().block(ofSeconds(10))

        val mapByCode = allWeatherItems.filter { it.cityId != null && it.cityId!!.isNotBlank() }.associate {
            val cityId = it.cityId
            val cityName = it.cityName
            "/$cityId" to Menu("Get weather for '$cityName'") {
                val weatherResponse = weatherService.getWeather(cityId = cityId!!).block(ofSeconds(10))
                "Weather for '$cityName' is [${weatherResponse?.main?.temp ?: "N/A"}]"
            }
        }

        val mapByCoordinates = allWeatherItems.filter { it.lat != null && it.lon != null }.associate {
            val cityName = it.cityName
            "/$cityName" to Menu("Get weather for '$cityName'") {
                val weatherResponse = weatherService.getWeather(lat = it.lat!!, lon = it.lon!!).block(ofSeconds(10))
                "Weather for '$cityName' is [${weatherResponse?.main?.temp ?: "N/A"}]"
            }
        }
        return mapByCode + mapByCoordinates
    }

    override fun getMenuInlineKeyboardMarkup() = getDefaultVerticalMenuInlineKeyboardMarkup()
}
