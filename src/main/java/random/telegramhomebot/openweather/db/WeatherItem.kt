package random.telegramhomebot.openweather.db

import org.hibernate.annotations.GenericGenerator
import org.hibernate.annotations.Type
import java.util.*
import javax.persistence.*

@Entity
@Table(name = "weather_item")
class WeatherItem(
    var cityName: String,
    var cityId: String?,
    var lat: String?,
    var lon: String?
) {
    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Type(type = "org.hibernate.type.UUIDCharType")
    @Column(length = 36, columnDefinition = "varchar(36)", updatable = false, nullable = false)
    var id: UUID? = null

    constructor(cityName: String, lat: String?, lon: String?) : this(cityName, null, lat, lon)
    constructor(cityName: String, cityId: String?) : this(cityName, cityId, null, null)
    constructor() : this("", null, null, null)
}
