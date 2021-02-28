package random.telegramhomebot.config;

import lombok.RequiredArgsConstructor;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import java.util.List;

@RequiredArgsConstructor
@Service
public class ProfileService {
	public static final String NETWORK_MONITOR = "network-monitor";
	public static final String MOCK_BOT = "mock-bot";

	private final Environment environment;

	public boolean isNetworkMonitorProfileActive() {
		return List.of(environment.getActiveProfiles()).contains(NETWORK_MONITOR);
	}
}
