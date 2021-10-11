package random.telegramhomebot

import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import random.telegramhomebot.config.ProfileService

@ActiveProfiles(ProfileService.MOCK_BOT)
@SpringBootTest
internal class TelegramHomeBotApplicationTests {
    @Test
    fun contextLoads() {
    }
}
