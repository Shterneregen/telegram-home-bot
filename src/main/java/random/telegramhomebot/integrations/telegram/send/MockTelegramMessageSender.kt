package random.telegramhomebot.integrations.telegram.send
import dev.telegram.api.TelegramResponse
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Service
import java.util.concurrent.CompletableFuture

@Service
@ConditionalOnProperty(name = ["integrations.telegram.enabled"], havingValue = "false", matchIfMissing = true)
class MockTelegramMessageSender : TelegramMessageSender {
    override fun sendMessage(messageText: String): CompletableFuture<Void> = CompletableFuture.completedFuture(null)

    override fun sendMessage(
        messageText: String,
        chatId: Long,
        replyToMessageId: Long?,
    ) = Unit

    override fun send(
        chatId: Long,
        response: TelegramResponse,
        editMessageId: Long?,
    ) = Unit

    override fun answerCallback(id: String) = Unit
}
