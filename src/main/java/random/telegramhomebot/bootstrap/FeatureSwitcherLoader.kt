package random.telegramhomebot.bootstrap;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import random.telegramhomebot.model.FeatureSwitcher;
import random.telegramhomebot.repository.FeatureSwitcherRepository;
import random.telegramhomebot.services.FeatureSwitcherService;

import java.util.Arrays;

@Slf4j
@RequiredArgsConstructor
@Component
public class FeatureSwitcherLoader implements CommandLineRunner {

	private final FeatureSwitcherRepository featureSwitcherRepository;

	@Override
	public void run(String... args) {
		createFeatureSwitchers();
	}

	private void createFeatureSwitchers() {
		if (featureSwitcherRepository.count() == 0) {
			log.info("Filling FeatureSwitcher table...");
			featureSwitcherRepository.saveAll(Arrays.asList(
					new FeatureSwitcher(FeatureSwitcherService.Features.NEW_HOSTS_NOTIFICATION.name(), true),
					new FeatureSwitcher(FeatureSwitcherService.Features.REACHABLE_HOSTS_NOTIFICATION.name(), true),
					new FeatureSwitcher(FeatureSwitcherService.Features.NOT_REACHABLE_HOSTS_NOTIFICATION.name(), true)
			));
		}
	}
}
