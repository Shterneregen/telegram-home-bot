package random.telegramhomebot.scheduling;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.SystemUtils;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import random.telegramhomebot.config.ProfileService;
import random.telegramhomebot.model.Host;
import random.telegramhomebot.services.FeatureSwitcherService;
import random.telegramhomebot.services.HostExplorerService;
import random.telegramhomebot.services.HostService;
import random.telegramhomebot.services.MessageFormatService;
import random.telegramhomebot.services.MessageService;
import random.telegramhomebot.telegram.Bot;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static random.telegramhomebot.AppConstants.Messages.NEW_HOSTS_MSG;
import static random.telegramhomebot.AppConstants.Messages.REACHABLE_HOSTS_MSG;
import static random.telegramhomebot.AppConstants.Messages.UNREACHABLE_HOSTS_MSG;
import static random.telegramhomebot.utils.Utils.joinLists;

@Slf4j
@RequiredArgsConstructor
@Profile(ProfileService.NETWORK_MONITOR)
@Service
public class StateChangeScheduler {

	private final Bot bot;
	private final MessageFormatService messageFormatService;
	private final MessageService messageService;
	private final HostExplorerService hostExplorerService;
	private final HostService hostService;
	private final FeatureSwitcherService featureSwitcherService;

	@Async
	@Scheduled(fixedRateString = "${state.change.scheduled.time}", initialDelay = 20000)
	public void checkState() {
		if (!SystemUtils.IS_OS_LINUX) {
			log.warn("checkState is not implemented for non-linux systems");
			return;
		}
		List<Host> storedHosts = hostService.getAllHosts();
		List<Host> currentHosts = hostExplorerService.getCurrentHosts();

		if (CollectionUtils.isNotEmpty(currentHosts) && CollectionUtils.isEmpty(storedHosts)) {
			bot.sendMessage(messageFormatService.formHostsListTable(messageService.getMessage(NEW_HOSTS_MSG), currentHosts));
		}

		List<Host> newHosts = null;
		List<Host> reachableHosts = null;
		List<Host> notReachableHosts = null;
		if (CollectionUtils.isNotEmpty(currentHosts) && CollectionUtils.isNotEmpty(storedHosts)) {
			newHosts = hostService.getNewHosts(storedHosts, currentHosts);
			reachableHosts = hostService.getHostsThatBecameReachable(storedHosts, currentHosts);
			notReachableHosts = hostService.getHostsThatBecameNotReachable(storedHosts, currentHosts);

			notifyBotAboutHosts(newHosts, reachableHosts, notReachableHosts);

			if (CollectionUtils.isNotEmpty(notReachableHosts)) {
				hostService.saveAllHosts(notReachableHosts);
			}
		}

		if (CollectionUtils.isNotEmpty(currentHosts)) {
			hostService.saveAllHosts(currentHosts);
		}
		hostService.saveTimeLogForHosts(joinLists(newHosts, reachableHosts, notReachableHosts));
	}

	private void notifyBotAboutHosts(List<Host> newHosts, List<Host> reachableHosts, List<Host> notReachableHosts) {
		boolean newHostsNotificationEnabled = featureSwitcherService.newHostsNotificationsEnabled();
		boolean reachableHostsNotificationEnabled = featureSwitcherService.reachableHostsNotificationsEnabled();
		boolean notReachableHostsNotificationEnabled = featureSwitcherService.notReachableHostsNotificationsEnabled();
		if (!newHostsNotificationEnabled && !reachableHostsNotificationEnabled && !notReachableHostsNotificationEnabled) {
			return;
		}

		Map<String, List<Host>> hostsMessagesMap = new LinkedHashMap<>();
		if (newHostsNotificationEnabled) {
			hostsMessagesMap.put(messageService.getMessage(NEW_HOSTS_MSG), newHosts);
		}
		if (reachableHostsNotificationEnabled) {
			hostsMessagesMap.put(messageService.getMessage(REACHABLE_HOSTS_MSG), reachableHosts);
		}
		if (notReachableHostsNotificationEnabled) {
			hostsMessagesMap.put(messageService.getMessage(UNREACHABLE_HOSTS_MSG), notReachableHosts);
		}

		if (!hostsMessagesMap.isEmpty()) {
			bot.sendMessage(messageFormatService.formHostsListTable(hostsMessagesMap));
		}
	}
}
