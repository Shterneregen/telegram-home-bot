package random.telegramhomebot.openweather

class WeatherResponse(
    var coord: Coord,
    var weather: List<Weather>,
    var base: String,
    var main: Main,
    var visibility: Int,
    var wind: Wind,
    var clouds: Clouds,
    var dt: Long,
    var sys: Sys,
    var timezone: Int,
    var id: Int,
    var name: String,
    var cod: Int
)

class Coord(var lon: Double, var lat: Double)
class Weather(var id: Int, var main: String, var description: String, var icon: String)
class Main(
    var temp: Float,
    var feels_like: Float,
    var temp_min: Float,
    var temp_max: Float,
    var pressure: Int,
    var humidity: Int
)

class Wind(var speed: Int, var deg: Int)
class Clouds(var all: Int)
class Sys(
    var type: Int,
    var id: Int,
    var country: String,
    var sunrise: Long,
    var sunset: Long
)
