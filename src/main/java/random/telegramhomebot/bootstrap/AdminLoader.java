package random.telegramhomebot.bootstrap;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import random.telegramhomebot.auth.entities.AuthGroup;
import random.telegramhomebot.auth.repositories.AuthGroupRepository;
import random.telegramhomebot.auth.entities.User;
import random.telegramhomebot.auth.repositories.UserRepository;

import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Component
public class AdminLoader implements CommandLineRunner {

	@Value("${default.admin.login}")
	private String adminLogin;
	@Value("${default.admin.password}")
	private String adminPassword;

	private final UserRepository userRepository;
	private final AuthGroupRepository authGroupRepository;

	@Override
	public void run(String... args) {
		loadAdminUser();
	}

	private void loadAdminUser() {
		if (userRepository.count() == 0) {
			log.info("Creating admin user [{}]", adminLogin);

			userRepository.save(new User(adminLogin, adminPassword));
			authGroupRepository.saveAll(
					List.of(new AuthGroup(adminLogin, "USER"),
							new AuthGroup(adminLogin, "ADMIN")));
		}
	}
}
