package random.telegramhomebot.auth.services;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import random.telegramhomebot.auth.UserPrincipal;
import random.telegramhomebot.auth.db.entities.User;
import random.telegramhomebot.auth.db.repositories.UserRepository;
import random.telegramhomebot.auth.enums.AuthErrorCode;
import random.telegramhomebot.services.messages.MessageService;
import random.telegramhomebot.utils.NetUtils;

import javax.servlet.http.HttpServletRequest;

@Service
public class AppUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;
    private final LoginAttemptService loginAttemptService;
    private final HttpServletRequest request;
    private final MessageService messageService;

    public AppUserDetailsService(
            UserRepository userRepository, LoginAttemptService loginAttemptService, HttpServletRequest request,
            MessageService messageService) {
        this.userRepository = userRepository;
        this.loginAttemptService = loginAttemptService;
        this.request = request;
        this.messageService = messageService;
    }

    @Value("${login.blocking.time.in.minutes}")
    private int blockingTimeInMinutes;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        String ip = NetUtils.getClientIp(request);
        if (loginAttemptService.isBlocked(ip)) {
            throw new RuntimeException(messageService.getMessage(AuthErrorCode.USER_BLOCKED.getMessageCode(),
                    new Object[]{blockingTimeInMinutes / 60}));
        }

        User user = userRepository.findByUsername(username);
        if (user == null) {
            throw new UsernameNotFoundException("Cannot find username: " + username);
        }
        return new UserPrincipal(user);
    }
}
