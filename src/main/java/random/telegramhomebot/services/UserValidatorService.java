package random.telegramhomebot.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import random.telegramhomebot.telegram.BotProperties;

@RequiredArgsConstructor
@Slf4j
@Service
public class UserValidatorService {

    private final BotProperties botProperties;

    public boolean isAllowedUser(Long userId) {
        return isOwner(userId) || isHomeGroupUser(userId);
    }

    private boolean isOwner(Long userId) {
        boolean isOwner = userId == botProperties.getBotOwnerId().intValue();
        log.debug("Is owner: {}", isOwner);
        return isOwner;
    }

    private boolean isHomeGroupUser(Long userId) {
        boolean isHomeGroupUser = botProperties.getHomeGroupUserIds() != null
                && botProperties.getHomeGroupUserIds().contains(userId);
        log.debug("Is home group user: {}", isHomeGroupUser);
        return isHomeGroupUser;
    }
}
