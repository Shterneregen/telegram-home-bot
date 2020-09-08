package random.telegramnetworkclientnotifier.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
public class UserValidator {

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
