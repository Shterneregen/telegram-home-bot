package random.telegramhomebot.telegram;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.ws.rs.core.UriBuilder;
import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@RequiredArgsConstructor
@Slf4j
@Service
public class TelegramNotifier {

    private final BotProperties botProperties;

    public void sendMessageToChatBot(String sendMessage) {
        sendMessage(sendMessage, botProperties.getBotOwnerId().toString());
    }

    public void sendMessage(String message, String chatId) {

        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .version(HttpClient.Version.HTTP_2)
                .build();

        UriBuilder builder = UriBuilder
                .fromUri(botProperties.getApiUrl())
                .path("/{token}/sendMessage")
                .queryParam("chat_id", chatId)
                .queryParam("text", message);

        HttpRequest request = HttpRequest.newBuilder()
                .GET()
                .uri(builder.build("bot" + botProperties.getToken()))
                .timeout(Duration.ofSeconds(5))
                .build();

        HttpResponse<String> response;
        try {
            response = client.send(request, HttpResponse.BodyHandlers.ofString());
            log.debug(String.valueOf(response.statusCode()));
            log.debug(String.valueOf(response.body()));
        } catch (IOException | InterruptedException e) {
            log.error(e.getMessage(), e);
        }
    }
}
