import dev.telegram.api.OutgoingTelegramMessage
import dev.telegram.api.TelegramEventHandler
import dev.telegram.api.TelegramRequestRejectedException
import dev.telegram.tdlight.*
import java.nio.file.Path
import java.util.concurrent.CompletionException

/** Offline denial-path smoke: no JNI, Telegram credentials, DNS or network connections. */
fun main() {
    var terminalFailures = 0
    val config = TdlightClientConfig(
        apiId = 1, apiHash = "a".repeat(32), dataDir = Path.of("build/offline-session"),
        proxy = MtprotoProxyConfig("proxy.invalid", secret = "ee" + "a".repeat(34)),
        // No default chat is needed by a library consumer.
    )
    val events = TelegramEventHandler { event -> check(event.userId > 0) }
    val denyAllOffline = NetworkEgressVerifier { error("Offline smoke intentionally supplies no OS attestation") }
    TdlightClient(config, TdlightBotCredentials("123:offline"), events, denyAllOffline,
        onFailure = { terminalFailures++ }).use { client ->
        val pending = client.send(OutgoingTelegramMessage(42, "explicit destination"))
        check(!pending.isDone)
        check(runCatching { client.start() }.isFailure)
        check(client.state() == TdlightConnectionState.FAILED)
        check(terminalFailures == 1)
        try {
            pending.join()
            error("Send must be rejected without OS attestation")
        } catch (e: CompletionException) {
            check(e.cause is TelegramRequestRejectedException)
        }
        client.stop()
    }
    println("Independent consumer offline smoke passed")
}
