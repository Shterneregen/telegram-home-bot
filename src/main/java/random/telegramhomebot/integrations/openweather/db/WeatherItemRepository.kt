package random.telegramhomebot.integrations.openweather.db

import org.springframework.data.jpa.repository.JpaRepository

interface WeatherItemRepository : JpaRepository<WeatherItem, Long>
