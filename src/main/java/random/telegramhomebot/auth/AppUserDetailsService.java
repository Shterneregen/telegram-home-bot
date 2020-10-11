package random.telegramhomebot.auth;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

@Service
public class AppUserDetailsService implements UserDetailsService {

	@Resource
	private UserRepository userRepository;
	@Resource
	private AuthGroupRepository authGroupRepository;

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
