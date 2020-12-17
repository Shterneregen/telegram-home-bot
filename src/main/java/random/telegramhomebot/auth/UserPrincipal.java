package random.telegramhomebot.auth;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import random.telegramhomebot.auth.entities.AuthGroup;
import random.telegramhomebot.auth.entities.User;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class UserPrincipal implements UserDetails {

	private User user;
	private List<AuthGroup> authGroups;

	public UserPrincipal(User user, List<AuthGroup> authGroups) {
		this.user = user;
		this.authGroups = authGroups;
	}

	@Override
	public Collection<? extends GrantedAuthority> getAuthorities() {
		if (authGroups == null) {
			return Collections.emptySet();
		}
		Set<SimpleGrantedAuthority> grantedAuthorities = new HashSet<>();
		authGroups.forEach(group -> grantedAuthorities.add(new SimpleGrantedAuthority(group.getAuthGroup())));

		return grantedAuthorities;
	}

	@Override
	public String getPassword() {
		return user.getPassword();
	}

	@Override
	public String getUsername() {
		return user.getUsername();
	}

	@Override
	public boolean isAccountNonExpired() {
		return true;
	}

	@Override
	public boolean isAccountNonLocked() {
		return true;
	}

	@Override
	public boolean isCredentialsNonExpired() {
		return true;
	}

	@Override
	public boolean isEnabled() {
		return true;
	}
}
