package random.telegramhomebot.utils;

import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@Component
public class MessageConfigurer {

	@Resource
	private MessageSource messageSource;

	public String getMessage(String code, @Nullable Object[] args) {
		return messageSource.getMessage(code, args, LocaleContextHolder.getLocale());
	}

	public String getMessage(String code) {
		return getMessage(code, null);
	}
}
