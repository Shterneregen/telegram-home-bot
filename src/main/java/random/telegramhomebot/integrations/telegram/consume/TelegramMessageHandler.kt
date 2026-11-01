package random.telegramhomebot.integrations.telegram.consume
import dev.telegram.api.TelegramEvent
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Service
import random.telegramhomebot.integrations.telegram.menu.CallbackMenuService
import random.telegramhomebot.integrations.telegram.send.TelegramMessageSender
import random.telegramhomebot.services.UserValidatorService
import random.telegramhomebot.services.commands.CommandService
import random.telegramhomebot.utils.logger

@Service
@ConditionalOnProperty(name = ["integrations.telegram.enabled"], havingValue = "true")
class TelegramMessageHandler(
    private val validator: UserValidatorService,
    private val commands: CommandService,
    private val menus: CallbackMenuService,
    private val sender: TelegramMessageSender,
) : TelegramMessageConsumer {
    private val log = logger()
    private val seen =
        object : LinkedHashMap<String, Boolean>() {
            override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Boolean>?) = size > 4096
        }

    override fun consume(event: TelegramEvent) = consume(event, true)

    fun consumeAcknowledged(event: TelegramEvent) = consume(event, false)

    @Synchronized
    private fun consume(
        event: TelegramEvent,
        acknowledge: Boolean,
    ) {
        val now = java.time.Instant.now().epochSecond
        if (event.callbackId == null && (event.sentAt < now - 300 || event.sentAt > now + 60)) return
        val key = event.callbackId?.let { "c:$it" } ?: "m:${event.chatId}:${event.messageId}"
        if (seen.put(key, true) != null) return
        val allowed = validator.checkAccess(event.userId)
        if (acknowledge) event.callbackId?.let { sender.answerCallback(it) }
        if (!allowed) return
        try {
            if (event.callbackId != null) {
                sender.send(event.chatId, menus.getRequestResult(event.text), event.messageId)
            } else {
                val menu = menus.getMenuForCommand(event.text)
                if (menu != null) {
                    sender.send(event.chatId, menu)
                } else {
                    commands.executeCommandOnMachine(event.text.lowercase())?.let { sender.sendMessage(it, event.chatId, event.messageId) }
                }
            }
        } catch (e: Exception) {
            log.warn("Telegram command failed ({})", e.javaClass.simpleName)
        }
    }
}
