package random.telegramhomebot.openweather.db

import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface WeatherItemRepository : JpaRepository<WeatherItem, UUID>
