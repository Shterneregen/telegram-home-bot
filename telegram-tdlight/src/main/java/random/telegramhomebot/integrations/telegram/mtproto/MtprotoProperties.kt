package random.telegramhomebot.integrations.telegram.mtproto
import org.springframework.boot.context.properties.ConfigurationProperties
import java.nio.file.Path

@ConfigurationProperties("integrations.telegram.mtproto")
class MtprotoProperties {
    var apiId: Int = 0
    var apiHash: String = ""
    var proxyHost: String = ""
    var proxyPort: Int = 443
    var proxySecret: String = ""
    var dataDir: Path = Path.of("data/tdlib")

    fun validate() {
        require(apiId > 0) { "TELEGRAM_API_ID must be positive" }
        require(apiHash.matches(Regex("[0-9a-fA-F]{32}"))) { "TELEGRAM_API_HASH must be a 32-character hexadecimal value" }
        require(
            proxyHost.isNotBlank() && !proxyHost.contains('/') &&
                !proxyHost.any {
                    it.isWhitespace()
                },
        ) { "TELEGRAM_PROXY_HOST must be a host or IP" }
        require(proxyPort in 1..65535) { "TELEGRAM_PROXY_PORT must be between 1 and 65535" }
        require(proxySecret.matches(Regex("(?i)ee[0-9a-f]{34,396}")) && proxySecret.length % 2 == 0) {
            "TELEGRAM_PROXY_SECRET must contain the full ee FakeTLS key and encoded domain"
        }
    }
}
