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
                val weatherResponse = weatherService.getWeather(cityId = cityId!!).block()
                "Weather for '$cityName' is [${weatherResponse?.main?.temp ?: "N/A"}]"
            }
        }

        val mapByCoordinates = allWeatherItems.filter { it.lat != null && it.lon != null }.associate {
            val cityName = it.cityName
            "/$cityName" to Menu("Get weather for '$cityName'") {
                val weatherResponse = weatherService.getWeather(lat = it.lat!!, lon = it.lon!!).block()
                "Weather for '$cityName' is [${weatherResponse?.main?.temp ?: "N/A"}]"
            }
        }
        return mapByCode + mapByCoordinates
    }

    override fun getMenuInlineKeyboardMarkup() = getDefaultVerticalMenuInlineKeyboardMarkup()
}
