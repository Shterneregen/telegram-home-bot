package random.telegramhomebot.integrations.telegram.mtproto

import dev.telegram.api.OutgoingTelegramMessage
import dev.telegram.api.TelegramKeyboard
import dev.telegram.api.TelegramResponse
import dev.telegram.tdlight.TdlightClient
import dev.telegram.tdlight.TdlightConnectionState
import org.springframework.beans.factory.DisposableBean
import org.springframework.boot.actuate.health.Health
import org.springframework.boot.actuate.health.HealthIndicator
import org.springframework.context.SmartLifecycle
import random.telegramhomebot.integrations.telegram.send.TelegramMessageSender
import java.util.concurrent.CompletableFuture

/** THB compatibility and lifecycle policy. No native SDK types cross this boundary. */
class TdlightTransport(
    private val createClient: () -> TdlightClient,
    private val commandButtons: () -> List<String>,
    private val buttonsInRow: Int,
    private val validateProperties: () -> Unit = {},
) : TelegramMessageSender, SmartLifecycle, HealthIndicator, DisposableBean {
    private val client by lazy(createClient)

    override fun isRunning() = client.isRunning()

    override fun start() {
        validateProperties()
        client.start()
    }

    override fun stop() = client.stop()

    override fun destroy() = stop()

    override fun health(): Health =
        when (client.state()) {
            TdlightConnectionState.FAILED -> Health.down().withDetail("transport", "failed").build()
            TdlightConnectionState.STOPPED -> Health.down().withDetail("transport", "stopped").build()
            TdlightConnectionState.INITIALIZING -> Health.outOfService().withDetail("transport", "initializing").build()
            TdlightConnectionState.DISCONNECTED -> Health.outOfService().withDetail("transport", "disconnected").build()
            TdlightConnectionState.READY -> Health.up().build()
        }

    private fun keyboard() =
        commandButtons().chunked(
            buttonsInRow.coerceAtLeast(1),
        ).takeIf { it.isNotEmpty() }?.let(TelegramKeyboard::Reply)

    override fun sendMessage(messageText: String): CompletableFuture<Void> =
        if (messageText.isBlank()) {
            CompletableFuture.completedFuture(null)
        } else {
            val request = client.sendToDefault(messageText, keyboard())
            CompletableFuture<Void>().also { result ->
                request.whenComplete { _, error ->
                    if (error == null) result.complete(null) else result.completeExceptionally(error)
                }
                result.whenComplete { _, _ ->
                    if (result.isCancelled) request.cancel(false)
                }
            }
        }

    override fun sendMessage(
        messageText: String,
        chatId: Long,
        replyToMessageId: Long?,
    ) {
        if (messageText.isNotBlank()) client.send(OutgoingTelegramMessage(chatId, messageText, replyToMessageId, keyboard()))
    }

    override fun send(
        chatId: Long,
        response: TelegramResponse,
        editMessageId: Long?,
    ) {
        val keyboard = response.buttons?.let(TelegramKeyboard::Inline)
        if (editMessageId == null) {
            client.send(OutgoingTelegramMessage(chatId, response.text, keyboard = keyboard))
        } else {
            client.edit(chatId, editMessageId, response.text, keyboard)
        }
    }

    override fun answerCallback(id: String) {
        client.answerCallback(id)
    }
}
