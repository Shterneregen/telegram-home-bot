package random.telegramhomebot.telegram

import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service
import random.telegramhomebot.config.ProfileService
import random.telegramhomebot.utils.logger
import java.io.IOException
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import javax.ws.rs.core.UriBuilder

@Deprecated(message = "This class is not used. It's here to remind you how to use a bot without a library")
@Profile("!${ProfileService.MOCK_BOT}")
@Service
class TelegramNotifier(private val botProperties: BotProperties) {
    val log = logger()

    fun sendMessageToChatBot(sendMessage: String?) {
        sendMessage(sendMessage, botProperties.botOwnerId.toString())
    }

    fun sendMessage(message: String?, chatId: String?) {
        val client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .version(HttpClient.Version.HTTP_2)
            .build()
        val builder = UriBuilder
            .fromUri(botProperties.apiUrl)
            .path("/{token}/sendMessage")
            .queryParam("chat_id", chatId)
            .queryParam("text", message)
        val request = HttpRequest.newBuilder()
            .GET()
            .uri(builder.build("bot${botProperties.token}"))
            .timeout(Duration.ofSeconds(5))
            .build()
        val response: HttpResponse<String>
        try {
            response = client.send(request, HttpResponse.BodyHandlers.ofString())
            log.debug(response.statusCode().toString())
            log.debug(response.body().toString())
        } catch (e: IOException) {
            log.error(e.message, e)
        } catch (e: InterruptedException) {
            log.error(e.message, e)
        }
    }
}
