package dev.telegram.tdlight

import java.nio.file.Path

/** Secrets deliberately use ordinary classes with no generated toString/copy/component methods. */
class MtprotoProxyConfig(val host: String, val port: Int = 443, val secret: String)

/**
 * Settings validated by [TdlightClient.start] before opening the session or loading native code.
 *
 * @property apiId Telegram application ID, separate from the bot identity.
 * @property apiHash Secret application hash; this class deliberately has no generated value rendering.
 * @property dataDir Persistent session directory exclusively locked while the native client is alive.
 * @property proxy MTProto endpoint and full ee-prefixed FakeTLS secret, including its encoded domain.
 * @property defaultChatId Optional notification destination; when present, readiness waits for its resolution.
 * @property tuning Queue bounds and request/retry timing.
 */
class TdlightClientConfig(
    val apiId: Int,
    val apiHash: String,
    val dataDir: Path,
    val proxy: MtprotoProxyConfig,
    val defaultChatId: Long? = null,
    val tuning: TdlightTuning = TdlightTuning(),
) {
    internal fun validate() {
        require(apiId > 0) { "API ID must be positive" }
        require(apiHash.matches(Regex("[0-9a-fA-F]{32}"))) { "API hash must be 32 hexadecimal characters" }
        require(proxy.host.isNotBlank() && !proxy.host.contains('/') && !proxy.host.any { it.isWhitespace() }) { "Invalid proxy host" }
        require(proxy.port in 1..65535) { "Invalid proxy port" }
        require(proxy.secret.matches(Regex("(?i)ee[0-9a-f]{34,396}")) && proxy.secret.length % 2 == 0) { "Full ee FakeTLS secret required" }
    }
}

/**
 * Outgoing/ACK limits include native requests and completion callbacks that have not returned.
 *
 * @property requestTimeoutMillis Deadline after submission; queued outgoing requests have no deadline.
 * @property startupRetryMillis Initial retry delay for transient identity/chat lookup errors, capped at 60 seconds.
 * @property outgoingCapacity Maximum admitted ordinary outgoing requests.
 * @property acknowledgementCapacity Separate admission capacity reserved for callback ACKs.
 * @property eventCapacity Waiting business events; overflow drops the new event without replay.
 * @property controlCapacity Waiting control tasks; overflow is a terminal transport failure.
 */
data class TdlightTuning(
    val requestTimeoutMillis: Long = 60_000,
    val startupRetryMillis: Long = 5_000,
    val outgoingCapacity: Int = 100,
    val acknowledgementCapacity: Int = 16,
    val eventCapacity: Int = 100,
    val controlCapacity: Int = 1024,
) {
    init {
        require(requestTimeoutMillis > 0 && startupRetryMillis in 1..60_000)
        require(listOf(outgoingCapacity, acknowledgementCapacity, eventCapacity, controlCapacity).all { it > 0 })
    }
}

/** Requested proxy identity before the verifier supplies its pinned numeric address. */
data class ProxyEndpoint(val host: String, val port: Int)

/** Policy-verified address; [requested] must exactly match the endpoint passed to the verifier. */
data class VerifiedProxyEndpoint(val requested: ProxyEndpoint, val address: String)

/**
 * Must attest effective OS-level policy for the current process before native initialization.
 * Return a numeric IP covered by that policy, retaining the requested host/port identity.
 * Do not cache attestations across processes or policy changes. The core validates/normalizes
 * the literal (no DNS; no IPv6 zones/brackets), but cannot enforce or prove OS egress policy.
 */
fun interface NetworkEgressVerifier {
    /** Returns the enforced endpoint or throws to reject startup before native initialization. */
    fun verify(endpoint: ProxyEndpoint): VerifiedProxyEndpoint
}

/** Readiness snapshot; STOPPED also describes a client that has never started, and FAILED is terminal. */
enum class TdlightConnectionState { STOPPED, INITIALIZING, DISCONNECTED, READY, FAILED }

/** Sanitized terminal failure stage; native exceptions and credentials are deliberately omitted. */
data class TdlightFailure(val stage: String)
