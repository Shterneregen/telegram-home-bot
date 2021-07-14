package random.telegramhomebot.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.task.SimpleAsyncTaskExecutor
import org.springframework.core.task.TaskExecutor
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.scheduling.annotation.EnableScheduling

@EnableAsync
@EnableScheduling
@Configuration
class TaskConfig {
    @Bean
    fun taskExecutor(): TaskExecutor = SimpleAsyncTaskExecutor()
}
