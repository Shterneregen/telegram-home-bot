package random.telegramhomebot.openweather.db

import org.springframework.data.jpa.repository.JpaRepository
import java.util.*

interface WeatherItemRepository : JpaRepository<WeatherItem, UUID> {
}