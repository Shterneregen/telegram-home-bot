package random.telegramhomebot.auth.services;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class LoginAttemptConfig {

    @Value("${login.blocking.time.in.minutes}")
    private int blockingTimeInMinutes;
    @Value("${login.max.attempts.before.block}")
    private int maxAttempts;

    @Bean
    public LoginAttemptService loginAttemptService() {
        return new LoginAttemptService(blockingTimeInMinutes, maxAttempts);
    }

    @Bean
    public LoginAttemptService botAttemptService() {
        return new LoginAttemptService(blockingTimeInMinutes, maxAttempts);
    }
}
