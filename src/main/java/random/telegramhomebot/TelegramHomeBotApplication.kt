package random.telegramhomebot

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class TelegramHomeBotApplication

fun main(args: Array<String>) {
    runApplication<TelegramHomeBotApplication>(*args)
}
