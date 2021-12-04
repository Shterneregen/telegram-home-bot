package random.telegramhomebot.services.menu

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Service
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton
import random.telegramhomebot.openweather.WeatherService
import random.telegramhomebot.openweather.db.WeatherItemRepository
import random.telegramhomebot.services.menu.dto.Menu

@ConditionalOnProperty(prefix = "openweather", value = ["enabled"], havingValue = "true")
@Service
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
            "/${cityId}" to Menu("Get weather for '${cityName}'") {
                val weatherResponse = weatherService.getWeather(cityId = cityId!!).block()
                "Weather for '${cityName}' is [${weatherResponse?.main?.temp ?: "N/A"}]"
            }
        }

        val mapByCoordinates = allWeatherItems.filter { it.lat != null && it.lon != null }.associate {
            val cityName = it.cityName
            "/$cityName" to Menu("Get weather for '${cityName}'") {
                val weatherResponse = weatherService.getWeather(lat = it.lat!!, lon = it.lon!!).block()
                "Weather for '${cityName}' is [${weatherResponse?.main?.temp ?: "N/A"}]"
            }
        }
        return mapByCode + mapByCoordinates
    }

    override fun getMenuInlineKeyboardMarkup(): InlineKeyboardMarkup {
        val rowList: List<List<InlineKeyboardButton>> = getMenuMap().entries
            .map { (command, menu) ->
                InlineKeyboardButton.builder()
                    .text(menu.buttonText)
                    .callbackData(command).build()
            }.map { listOf(it) }
        return InlineKeyboardMarkup.builder().keyboard(rowList).build()
    }
}
