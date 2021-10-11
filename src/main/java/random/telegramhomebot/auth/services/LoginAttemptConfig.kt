package random.telegramhomebot.auth.services

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class LoginAttemptConfig {

    @Value("\${login.blocking.time.in.minutes}")
    private val blockingTimeInMinutes = 0

    @Value("\${login.max.attempts.before.block}")
    private val maxAttempts = 0

    @Bean
    fun loginAttemptService() = LoginAttemptService(blockingTimeInMinutes, maxAttempts)

    @Bean
    fun botAttemptService() = LoginAttemptService(blockingTimeInMinutes, maxAttempts)
}
