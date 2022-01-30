package random.telegramhomebot.services.messages

import org.springframework.context.MessageSource
import org.springframework.lang.Nullable
import org.springframework.stereotype.Service
import java.util.Locale

// import javax.servlet.http.HttpServletRequest

@Service
class MessageService(
    private val messageSource: MessageSource,
//    private val localeResolver: LocaleResolver,
    private val defaultLocale: Locale
) {
    fun getMessage(code: String) = getMessage(code, null as Array<Any?>?)
    fun getMessage(code: String, @Nullable args: Array<Any?>?): String = getMessage(code, args, defaultLocale)

//    fun getMessage(code: String, request: ServerHttpRequest): String =
//        getMessage(code, null, localeResolver.resolveLocale(request))

//    fun getMessage(code: String, @Nullable args: Array<Any?>?, request: HttpServletRequest): String =
//        getMessage(code, args, localeResolver.resolveLocale(request))

    fun getMessage(code: String, locale: Locale): String = messageSource.getMessage(code, null, locale)
    fun getMessage(code: String, args: Array<Any?>?, locale: Locale): String =
        messageSource.getMessage(code, args, locale)
}
