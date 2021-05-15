package random.telegramhomebot.telegram;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Data
@Configuration
@ConfigurationProperties(prefix = "telegram")
public class BotProperties {
    private String apiUrl;
    private Long botOwnerId;
    private String token;
    private String botName;
    private List<Long> homeGroupUserIds;
}
