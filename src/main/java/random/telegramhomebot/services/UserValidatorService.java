package random.telegramhomebot.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.lang.invoke.MethodHandles;
import java.util.List;

@Service
public class UserValidatorService {

	private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

	@Value("${telegram.bot.chat.id}")
	private Long botChatId;
	@Value("${home.group.user.ids}")
	private List<Integer> homeGroupUserIds;

	public boolean isAllowedUser(Integer userId) {
		return isOwner(userId) || isHomeGroupUser(userId);
	}

	private boolean isOwner(Integer userId) {
		boolean isOwner = userId == botChatId.intValue();
		log.debug("Is owner: {}", isOwner);
		return isOwner;
	}

	private boolean isHomeGroupUser(Integer userId) {
		boolean isHomeGroupUser = homeGroupUserIds != null && homeGroupUserIds.contains(userId);
		log.debug("Is home group user: {}", isHomeGroupUser);
		return isHomeGroupUser;
	}
}
