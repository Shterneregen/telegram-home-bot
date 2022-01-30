package random.telegramhomebot.config

import io.r2dbc.h2.H2ConnectionConfiguration
import io.r2dbc.h2.H2ConnectionFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration
import org.springframework.data.r2dbc.config.AbstractR2dbcConfiguration
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories

@Configuration
@EnableR2dbcRepositories
internal class R2DBCConfiguration : AbstractR2dbcConfiguration() {

    @Value("\${spring.r2dbc.url}")
    private lateinit var url: String

    @Value("\${spring.r2dbc.name}")
    private lateinit var username: String

    override fun connectionFactory() = H2ConnectionFactory(
        H2ConnectionConfiguration.builder()
            .url(url)
            .username(username)
            .build()
    )
}
