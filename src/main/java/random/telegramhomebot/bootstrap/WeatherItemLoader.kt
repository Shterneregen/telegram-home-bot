package random.telegramhomebot.bootstrap

import org.springframework.boot.CommandLineRunner
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import random.telegramhomebot.openweather.db.WeatherItem
import random.telegramhomebot.openweather.db.WeatherItemRepository
import random.telegramhomebot.utils.logger

@ConditionalOnProperty(prefix = "openweather", value = ["enabled"], havingValue = "true")
@Order(4)
@Component
class WeatherItemLoader(private val weatherItemRepository: WeatherItemRepository) : CommandLineRunner {
    private val log = logger()

    override fun run(vararg args: String) {
        log.info("Filling weather items table...")
        val weatherItemsCount = weatherItemRepository.count().block()
        if (weatherItemsCount == 0L) {
            weatherItemRepository.saveAll(
                listOf(
                    WeatherItem("SPb", "498817"),
                    WeatherItem("Pushkin", "00.000000", "00.000000")
                )
            ).subscribe()
        }
    }
}
