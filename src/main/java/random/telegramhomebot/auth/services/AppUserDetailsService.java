package random.telegramhomebot.auth.services;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import random.telegramhomebot.auth.AuthErrorCodes;
import random.telegramhomebot.auth.UserPrincipal;
import random.telegramhomebot.auth.entities.User;
import random.telegramhomebot.auth.repositories.UserRepository;
import random.telegramhomebot.services.MessageService;
import random.telegramhomebot.utils.NetUtils;

import javax.servlet.http.HttpServletRequest;

@RequiredArgsConstructor
@Service
public class AppUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;
    private final LoginAttemptService loginAttemptService;
    private final HttpServletRequest request;
    private final MessageService messageService;

    @Value("${login.blocking.time.in.minutes}")
    private int blockingTimeInMinutes;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        String ip = NetUtils.getClientIp(request);
        if (loginAttemptService.isBlocked(ip)) {
            throw new RuntimeException(messageService.getMessage(AuthErrorCodes.USER_BLOCKED.getErrorMessageCode(),
                    new Object[]{blockingTimeInMinutes / 60}));
        }

        User user = userRepository.findByUsername(username);
        if (user == null) {
            throw new UsernameNotFoundException("Cannot find username: " + username);
        }
        return new UserPrincipal(user);
    }
}
