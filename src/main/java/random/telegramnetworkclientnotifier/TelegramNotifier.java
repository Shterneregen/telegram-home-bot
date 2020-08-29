package random.telegramnetworkclientnotifier;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.ws.rs.core.UriBuilder;
import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@Slf4j
@Service
public class TelegramNotifier {

	@Value("${telegram.api.url}")
	private String telegramApiUrl;
	@Value("${telegram.bot.chat.id}")
	private String botChatId;
	@Value("${telegram.private.group.chat.id}")
	private String privateGroupChatId;
	@Value("${telegram.token}")
	private String token;

	public void sendMessageToChatBot(String sendMessage) {
		sendMessage(sendMessage, botChatId);
	}

	public void sendMessageToPrivateGroup(String sendMessage) {
		sendMessage(sendMessage, privateGroupChatId);
	}

	public void sendMessage(String message, String chatId) {

		HttpClient client = HttpClient.newBuilder()
				.connectTimeout(Duration.ofSeconds(5))
				.version(HttpClient.Version.HTTP_2)
				.build();

		UriBuilder builder = UriBuilder
				.fromUri(telegramApiUrl)
				.path("/{token}/sendMessage")
				.queryParam("chat_id", chatId)
				.queryParam("text", message);

		HttpRequest request = HttpRequest.newBuilder()
				.GET()
				.uri(builder.build("bot" + token))
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
