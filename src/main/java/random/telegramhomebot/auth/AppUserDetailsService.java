package random.telegramhomebot.auth;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import random.telegramhomebot.auth.entities.AuthGroup;
import random.telegramhomebot.auth.entities.User;
import random.telegramhomebot.auth.repositories.AuthGroupRepository;
import random.telegramhomebot.auth.repositories.UserRepository;

import java.util.List;

@RequiredArgsConstructor
@Service
public class AppUserDetailsService implements UserDetailsService {

	private final UserRepository userRepository;
	private final AuthGroupRepository authGroupRepository;

	@Override
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
		User user = userRepository.findByUsername(username);
		if (user == null) {
			throw new UsernameNotFoundException("Cannot find username: " + username);
		}
		List<AuthGroup> authGroups = authGroupRepository.findByUsername(username);
		return new UserPrincipal(user, authGroups);
	}
}
