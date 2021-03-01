package random.telegramhomebot.services;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.LocaleResolver;

import javax.servlet.http.HttpServletRequest;
import java.util.Locale;

@RequiredArgsConstructor
@Service
public class MessageService {

	private final MessageSource messageSource;
	private final LocaleResolver localeResolver;
	private final Locale defaultLocale;

	@Value("${default.language}")
	private String defaultLanguage;

	public String getMessage(String code, @Nullable Object[] args) {
		return getMessage(code, args, defaultLocale);
	}

	public String getMessage(String code) {
		return getMessage(code, (Object[]) null);
	}

	public String getMessage(String code, HttpServletRequest request) {
		return getMessage(code, null, localeResolver.resolveLocale(request));
	}

	public String getMessage(String code, @Nullable Object[] args, HttpServletRequest request) {
		return getMessage(code, args, localeResolver.resolveLocale(request));
	}

	public String getMessage(String code, Locale locale) {
		return messageSource.getMessage(code, null, locale);
	}

	public String getMessage(String code, @Nullable Object[] args, Locale locale) {
		return messageSource.getMessage(code, args, locale);
	}
}
