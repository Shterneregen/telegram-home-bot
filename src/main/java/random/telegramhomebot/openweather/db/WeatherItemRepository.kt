package random.telegramhomebot.openweather.db

import org.springframework.data.r2dbc.repository.R2dbcRepository
import java.util.UUID

interface WeatherItemRepository : R2dbcRepository<WeatherItem, UUID>
