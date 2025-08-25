package random.telegramhomebot.services.messages

import jakarta.servlet.http.HttpServletRequest
import org.springframework.context.MessageSource
import org.springframework.lang.Nullable
import org.springframework.stereotype.Service
import org.springframework.web.servlet.LocaleResolver
import java.util.*

@Service
class MessageService(
    private val messageSource: MessageSource,
    private val localeResolver: LocaleResolver,
    private val defaultLocale: Locale
) {
    fun getMessage(code: String, request: HttpServletRequest): String =
        getMessage(code, null, localeResolver.resolveLocale(request))

    fun getMessage(code: String, @Nullable args: Array<Any?>?, request: HttpServletRequest): String =
        getMessage(code, args, localeResolver.resolveLocale(request))

    fun getMessage(code: String, args: Array<Any?>? = null, locale: Locale = defaultLocale): String =
        messageSource.getMessage(code, args, locale)
}
