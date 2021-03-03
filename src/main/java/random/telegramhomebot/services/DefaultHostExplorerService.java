package random.telegramhomebot.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import random.telegramhomebot.config.ProfileService;
import random.telegramhomebot.model.Host;
import random.telegramhomebot.telegram.Bot;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static random.telegramhomebot.utils.Utils.comparingByIp;

@Slf4j
@RequiredArgsConstructor
@Profile(ProfileService.NETWORK_MONITOR)
@Service
public class DefaultHostExplorerService implements HostExplorerService {

	private final CommandRunnerService commandRunnerService;
	private final Bot bot;
	private final ObjectMapper objectMapper;
	private final HostService hostService;

	@Value("${state.change.command}")
	private String stateChangeCommand;

	@Override
	public List<Host> getCurrentHosts() {
		List<String> hostsJson = commandRunnerService.runCommand(stateChangeCommand);
		List<Host> currentHosts = null;
		try {
			currentHosts = objectMapper.readValue(hostsJson.get(0), new TypeReference<>() {
			});
		} catch (JsonProcessingException e) {
			log.error(e.getMessage(), e);
			bot.sendMessage(e.getMessage());
		}
		return currentHosts != null && currentHosts.size() > 0
				? currentHosts.stream().filter(host -> host.getMac() != null).peek(this::fillHostStoredInfo)
				.sorted(comparingByIp()).collect(Collectors.toList())
				: Collections.emptyList();
	}

	private void fillHostStoredInfo(Host currentHost) {
		Optional<Host> storedHost = hostService.getHostByMac(currentHost.getMac());
		if (storedHost.isPresent()) {
			currentHost.setId(storedHost.get().getId());
			currentHost.setDeviceName(storedHost.get().getDeviceName());
		}
	}
}
