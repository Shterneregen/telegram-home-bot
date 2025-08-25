package random.telegramhomebot.integrations.openweather.db

import jakarta.persistence.*

@Entity
@Table(name = "weather_item")
class WeatherItem(
    var cityName: String,
    var cityId: String?,
    var lat: String?,
    var lon: String?
) {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "weather_item_seq")
    @SequenceGenerator(name = "weather_item_seq", sequenceName = "weather_item_seq", allocationSize = 1)
    @Column(updatable = false, nullable = false)
    var id: Long? = null

    constructor(cityName: String, lat: String?, lon: String?) : this(cityName, null, lat, lon)
    constructor(cityName: String, cityId: String?) : this(cityName, cityId, null, null)
    constructor() : this("", null, null, null)
}
