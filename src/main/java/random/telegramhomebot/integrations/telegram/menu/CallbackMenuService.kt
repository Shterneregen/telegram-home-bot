package random.telegramhomebot.integrations.telegram.menu
import dev.telegram.api.TelegramResponse
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Service

@Service
@ConditionalOnProperty(name = ["integrations.telegram.enabled"], havingValue = "true")
class CallbackMenuService(private val menuServices: List<MenuService>) {
    fun getRequestResult(command: String): TelegramResponse =
        menuServices.firstNotNullOfOrNull { service ->
            service.getMenuMap()[command]?.let { TelegramResponse(it.method.get(), service.getMenuInlineKeyboardMarkup()) }
        } ?: TelegramResponse("No answer")

    fun getMenuForCommand(text: String): TelegramResponse? =
        menuServices.find { it.menuCommand == text }
            ?.let { TelegramResponse(it.menuText, it.getMenuInlineKeyboardMarkup()) }
}
