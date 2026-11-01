package dev.telegram.tdlight

import dev.telegram.api.OutgoingTelegramMessage
import dev.telegram.api.TelegramDeliveryEvent
import dev.telegram.api.TelegramEvent
import dev.telegram.api.TelegramEventHandler
import dev.telegram.api.TelegramKeyboard
import dev.telegram.api.TelegramMessaging
import dev.telegram.api.TelegramRequestRejectedException
import dev.telegram.api.TelegramRequestResult
import dev.telegram.api.TelegramResponse
import it.tdlight.ClientFactory
import it.tdlight.Init
import it.tdlight.TelegramClient
import it.tdlight.client.TelegramError
import it.tdlight.jni.TdApi
import org.slf4j.LoggerFactory
import java.nio.channels.FileChannel
import java.nio.channels.FileLock
import java.nio.file.Files
import java.nio.file.StandardOpenOption
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionException
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.ScheduledThreadPoolExecutor
import java.util.concurrent.ThreadFactory
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import java.util.concurrent.atomic.AtomicBoolean

/**
 * One-shot client. Request outcomes are published on an owned daemon completion pool, never by
 * native, control, timeout or shutdown threads. Futures may complete after close returns.
 * A non-async continuation attached to an already completed future can run on the caller thread.
 * Keep continuations short and use a host executor for blocking I/O: admission capacity is retained
 * until both the native response and completion callbacks finish. Close does not join callbacks.
 * Delivery/failure listeners run on transport threads and must also remain non-blocking.
 */
class TdlightClient internal constructor(
    private val settings: TdlightClientConfig,
    private val bot: TdlightBotCredentials,
    private val handler: TelegramEventHandler,
    private val verifier: NetworkEgressVerifier,
    private val onFailure: (TdlightFailure) -> Unit,
    private val onDelivery: (TelegramDeliveryEvent) -> Unit,
    private val openFactory: () -> ClientFactory,
) : TelegramMessaging, AutoCloseable {
    /** Creates a single-use client; native initialization is deferred until [start]. */
    constructor(
        settings: TdlightClientConfig,
        bot: TdlightBotCredentials,
        handler: TelegramEventHandler,
        verifier: NetworkEgressVerifier,
        onFailure: (TdlightFailure) -> Unit = {},
        onDelivery: (TelegramDeliveryEvent) -> Unit = {},
    ) : this(settings, bot, handler, verifier, onFailure, onDelivery, {
        Init.init()
        it.tdlight.Log.setVerbosityLevel(0)
        ClientFactory.create()
    })

    private val log = LoggerFactory.getLogger(TdlightClient::class.java)
    private var factory: ClientFactory? = null
    private var client: TelegramClient? = null
    private var lockChannel: FileChannel? = null
    private var lock: FileLock? = null
    private val failed = AtomicBoolean(false)
    private val started = AtomicBoolean(false)
    private val stopped = AtomicBoolean(false)
    private val closed = CompletableFuture<Void>()
    private val identity = CompletableFuture<Void>()
    private val destination = CompletableFuture<Void>()
    private val outbox =
        TdlightOutbox({
            requireNotNull(client).sendAsync(it)
        }, settings.tuning.requestTimeoutMillis, settings.tuning.outgoingCapacity, settings.tuning.acknowledgementCapacity)
    private val retries =
        ScheduledThreadPoolExecutor(1) { task ->
            Thread(task, "tdlight-startup-retry").apply { isDaemon = true }
        }.apply { removeOnCancelPolicy = true }
    private val control =
        ThreadPoolExecutor(
            1,
            1,
            0,
            TimeUnit.SECONDS,
            ArrayBlockingQueue(settings.tuning.controlCapacity),
            ThreadFactory { Thread(it, "tdlight-control").apply { isDaemon = true } },
        )
    private val work =
        ThreadPoolExecutor(
            1,
            1,
            0,
            TimeUnit.SECONDS,
            ArrayBlockingQueue(settings.tuning.eventCapacity),
            ThreadFactory { Thread(it, "tdlight-events").apply { isDaemon = true } },
        )

    @Volatile private var stopping = false

    @Volatile private var running = false

    @Volatile private var connected = false

    @Volatile private var authorized = false
    private var proxyAddress = ""
    private var proxyReady = false
    private var auth: TdApi.AuthorizationState? = null
    private var authenticating = false
    private var checkingIdentity = false

    /** True while lifecycle processing is active, including initialization and temporary disconnection. */
    fun isRunning() = running

    /** Reports local readiness, not proof of end-to-end Telegram delivery. */
    fun state(): TdlightConnectionState =
        when {
            failed.get() -> TdlightConnectionState.FAILED
            stopping || !running -> TdlightConnectionState.STOPPED
            !authorized || !destination.isDone -> TdlightConnectionState.INITIALIZING
            !connected -> TdlightConnectionState.DISCONNECTED
            else -> TdlightConnectionState.READY
        }

    /**
     * Verifies egress policy and opens the native client; authorization completes asynchronously.
     * Repeated calls while running are harmless. After stop or failure, create a new instance.
     *
     * @throws IllegalStateException if this instance was consumed or startup fails; startup errors are sanitized.
     */
    @Synchronized
    fun start() {
        if (running) return
        check(!stopped.get() && started.compareAndSet(false, true)) { "TDLight lifecycle is one-shot; create a new client" }
        try {
            settings.validate()
            require(bot.token.isNotBlank()) { "Bot token is required" }
            val endpoint = ProxyEndpoint(settings.proxy.host, settings.proxy.port)
            val verified = verifier.verify(endpoint)
            check(verified.requested == endpoint) { "Verified proxy identity mismatch" }
            // Finish all egress checks before touching persistent state or loading JNI.
            proxyAddress = numericProxyAddress(verified.address)
            Files.createDirectories(settings.dataDir)
            if (Files.getFileStore(settings.dataDir).supportsFileAttributeView("posix")) {
                Files.setPosixFilePermissions(settings.dataDir, java.nio.file.attribute.PosixFilePermissions.fromString("rwx------"))
            }
            lockChannel = FileChannel.open(settings.dataDir.resolve("session.lock"), StandardOpenOption.CREATE, StandardOpenOption.WRITE)
            lock = requireNotNull(lockChannel?.tryLock()) { "TDLib session directory is already in use" }
            factory = openFactory()
            client = factory?.createClient()
            running = true
            client?.initialize(
                it.tdlight.ResultHandler<TdApi.Update> { update ->
                    // Always observe closure, including after terminal failure and during stop.
                    if (update is TdApi.UpdateAuthorizationState && update.authorizationState is TdApi.AuthorizationStateClosed) {
                        closed.complete(null)
                    }
                    if (running) onControl { onUpdate(update) }
                },
                { fail("update callback") },
                { fail("request callback") },
            )
        } catch (e: Throwable) {
            val firstFailure = failed.compareAndSet(false, true)
            stop()
            if (firstFailure) notifyFailure("startup")
            // Do not expose native/provider exceptions which may contain secrets.
            throw IllegalStateException("TDLight initialization failed; verify configuration, egress policy and native runtime")
        }
    }

    private fun <T : TdApi.Object> request(query: TdApi.Function<T>): CompletableFuture<T> =
        requireNotNull(client).sendAsync(query).orTimeout(settings.tuning.requestTimeoutMillis, TimeUnit.MILLISECONDS)

    private fun onUpdate(update: TdApi.Object) {
        when (update) {
            is TdApi.UpdateConnectionState -> {
                connected = update.state is TdApi.ConnectionStateReady
            }
            is TdApi.UpdateAuthorizationState -> {
                auth = update.authorizationState
                when (auth) {
                    is TdApi.AuthorizationStateWaitTdlibParameters -> initializeProxy()
                    is TdApi.AuthorizationStateClosed -> {
                        closed.complete(null)
                        fail("unexpected authorization closure")
                    }
                    is TdApi.AuthorizationStateWaitPhoneNumber, is TdApi.AuthorizationStateReady -> authorize()
                    is TdApi.AuthorizationStateClosing -> fail("unexpected authorization closing")
                    else -> fail("unsupported authorization state")
                }
            }
            is TdApi.UpdateNewMessage, is TdApi.UpdateNewCallbackQuery -> TdlightMessageMapper.event(update)?.let(::dispatch)
            is TdApi.UpdateMessageSendFailed ->
                notifyDelivery(
                    TelegramDeliveryEvent.Failed(update.message.chatId, update.oldMessageId, update.error?.code),
                )
            is TdApi.UpdateMessageSendSucceeded ->
                notifyDelivery(
                    TelegramDeliveryEvent.Sent(update.message.chatId, update.oldMessageId, update.message.id),
                )
        }
    }

    private fun initializeProxy() {
        val parameters =
            TdApi.SetTdlibParameters().apply {
                databaseDirectory = settings.dataDir.resolve("database").toString()
                filesDirectory = settings.dataDir.resolve("files").toString()
                apiId = settings.apiId
                apiHash = settings.apiHash
                systemLanguageCode = "en"
                deviceModel = "JVM"
                systemVersion = "Java 17"
                applicationVersion = "0.1.0"
                databaseEncryptionKey = byteArrayOf()
            }
        ProxyBootstrap { request(it).thenApply<TdApi.Object> { result -> result } }.initialize(
            TdApi.Proxy(proxyAddress, settings.proxy.port, TdApi.ProxyTypeMtproto(settings.proxy.secret)),
            parameters,
        ).whenComplete { _, error ->
            if (running) {
                onControl {
                    if (error != null) {
                        fail("proxy initialization")
                    } else {
                        proxyReady = true
                        authorize()
                    }
                }
            }
        }
    }

    private fun authorize() {
        if (!proxyReady || !running) return
        if (auth is TdApi.AuthorizationStateWaitPhoneNumber && !authenticating) {
            authenticating = true
            // One pending authentication request survives proxy outages; TDLib reconnects internally.
            requireNotNull(client).sendAsync(TdApi.CheckAuthenticationBotToken(bot.token)).whenComplete {
                    _,
                    error,
                ->
                if (error != null) onControl { fail("bot authentication") }
            }
        } else if (auth is TdApi.AuthorizationStateReady && !checkingIdentity) {
            checkingIdentity = true
            startupRequest(TdApi.GetMe(), "bot identity verification") { me ->
                if (me.type !is TdApi.UserTypeBot || me.id.toString() != bot.token.substringBefore(':')) {
                    fail("bot identity verification")
                } else {
                    authorized = true
                    identity.complete(null)
                    // Explicit destinations need only the bot identity; default sends have a second gate.
                    outbox.ready()
                    val defaultChat = settings.defaultChatId
                    if (defaultChat == null) {
                        destination.complete(null)
                        outbox.ready(destination = true)
                    } else {
                        startupRequest(TdApi.GetChat(defaultChat), "configured default chat") { chat ->
                            if (chat.id != defaultChat) {
                                fail("configured default chat")
                            } else {
                                destination.complete(null)
                                outbox.ready(destination = true)
                            }
                        }
                    }
                }
            }
        }
    }

    private fun <T : TdApi.Object> startupRequest(
        query: TdApi.Function<T>,
        stage: String,
        attempt: Int = 0,
        accept: (T) -> Unit,
    ) {
        if (!running) return
        // Keep one native request alive during outages; a Java timeout would not cancel it.
        requireNotNull(client).sendAsync(query).whenComplete { result, error ->
            onControl {
                val cause = if (error is CompletionException) error.cause else error
                when {
                    error == null -> accept(result)
                    cause is TimeoutException ||
                        (cause is TelegramError && (cause.errorCode in 500..599 || cause.errorCode == 429 || cause.errorCode == -1)) -> {
                        log.warn("TDLight {} temporarily unavailable; retrying", stage)
                        val delay = minOf(60_000, settings.tuning.startupRetryMillis * (1L shl attempt.coerceAtMost(4)))
                        retries.schedule(
                            { onControl { startupRequest(query, stage, (attempt + 1).coerceAtMost(4), accept) } },
                            delay,
                            TimeUnit.MILLISECONDS,
                        )
                    }
                    else -> fail(stage)
                }
            }
        }
    }

    private fun dispatch(event: TelegramEvent) {
        event.callbackId?.let { id ->
            // Require ACK admission before handing off the action, without waiting for a native response.
            val acknowledgement = outbox.trySubmit(TdlightMessageMapper.acknowledgement(id))
            if (acknowledgement == null) {
                log.warn("Telegram callback dropped: acknowledgement queue is full or closed")
                return
            }
            acknowledgement.whenComplete { _, error ->
                // A submitted ACK may have succeeded despite a timeout; never retry or replay the action.
                if (error != null) log.warn("Telegram callback acknowledgement failed ({})", error.javaClass.simpleName)
            }
        }
        try {
            work.execute {
                try {
                    identity.join()
                    if (!stopping && !failed.get()) handler.handle(event)
                } catch (e: Exception) {
                    log.warn("Telegram command processing failed ({})", e.javaClass.simpleName)
                }
            }
        } catch (
            e: RejectedExecutionException,
        ) {
            log.warn("Telegram command queue is full")
        }
    }

    private fun fail(stage: String) {
        if (!running || stopping || !failed.compareAndSet(false, true)) return
        running = false
        authorized = false
        outbox.close()
        retries.shutdownNow()
        identity.completeExceptionally(IllegalStateException("TDLight transport failed"))
        destination.completeExceptionally(IllegalStateException("TDLight transport failed"))
        log.error("TDLight terminal failure at {}", stage)
        // Cleanup is independent of the host's failure policy and cannot join the control worker.
        Thread({ stop() }, "tdlight-terminal-cleanup").apply { isDaemon = true }.start()
        notifyFailure(stage)
    }

    /**
     * Permanently stops admission and interrupts workers. Repeated calls are harmless.
     * Native close and factory cleanup wait up to 20 and 10 seconds respectively, after acquiring
     * the lifecycle monitor. User continuations are not joined; unfinished cleanup retains the session lock.
     */
    @Synchronized
    fun stop() {
        if (!stopped.compareAndSet(false, true)) return
        stopping = true
        running = false
        authorized = false
        outbox.close()
        retries.shutdownNow()
        identity.completeExceptionally(IllegalStateException("TDLight stopped"))
        destination.completeExceptionally(IllegalStateException("TDLight stopped"))
        work.shutdownNow()
        control.shutdownNow()
        try {
            client?.send(TdApi.Close(), null)
            if (client != null) closed.get(20, TimeUnit.SECONDS)
        } catch (e: Exception) {
            log.warn("TDLight close deadline exceeded")
        } finally {
            running = false
            releaseResources()
        }
    }

    /** Equivalent to [stop]; this instance cannot be restarted. */
    override fun close() = stop()

    private fun onControl(action: () -> Unit) {
        try {
            control.execute {
                if (running) {
                    try {
                        action()
                    } catch (_: Exception) {
                        fail("update processing")
                    }
                }
            }
        } catch (_: RejectedExecutionException) {
            if (running && !stopping) fail("control queue overflow")
        }
    }

    private fun releaseResources() {
        work.shutdownNow()
        control.shutdownNow()
        val released = CompletableFuture<Void>()
        Thread({
            try {
                factory?.close()
                lock?.release()
                lockChannel?.close()
                released.complete(null)
            } catch (e: Throwable) {
                released.completeExceptionally(e)
            }
        }, "tdlight-cleanup").apply { isDaemon = true }.start()
        try {
            released.get(10, TimeUnit.SECONDS)
        } catch (_: Exception) {
            // Never release a session lock while its native instance may still be alive.
            log.error("TDLight cleanup deadline exceeded; session lock retained until process exit")
        }
    }

    private fun notifyFailure(stage: String) {
        try {
            onFailure(TdlightFailure(stage))
        } catch (_: Exception) {
            log.warn("TDLight failure listener failed")
        }
    }

    private fun notifyDelivery(event: TelegramDeliveryEvent) {
        if (event is TelegramDeliveryEvent.Failed) log.warn("TDLight message delivery failed (code {})", event.errorCode)
        try {
            onDelivery(event)
        } catch (_: Exception) {
            log.warn("TDLight delivery listener failed")
        }
    }

    private fun operation(
        query: () -> TdApi.Function<*>,
        defaultDestination: Boolean = false,
    ): CompletableFuture<TelegramRequestResult> =
        try {
            outbox.submit(query(), defaultDestination).also { result ->
                result.whenComplete { _, error ->
                    if (error != null) log.warn("TDLight outgoing request failed ({})", error.javaClass.simpleName)
                }
            }
        } catch (_: IllegalArgumentException) {
            CompletableFuture.failedFuture(TelegramRequestRejectedException())
        }

    override fun send(message: OutgoingTelegramMessage): CompletableFuture<TelegramRequestResult> =
        operation({ TdlightMessageMapper.outgoing(message) })

    /**
     * Enqueues plain text for the configured default chat, waiting for its startup resolution.
     * Returns a rejected future when no default chat is configured; explicit sends do not require one.
     */
    fun sendToDefault(
        text: String,
        keyboard: TelegramKeyboard? = null,
    ): CompletableFuture<TelegramRequestResult> {
        val chatId = settings.defaultChatId ?: return CompletableFuture.failedFuture(TelegramRequestRejectedException())
        return operation({ TdlightMessageMapper.outgoing(OutgoingTelegramMessage(chatId, text, keyboard = keyboard)) }, true)
    }

    override fun edit(
        chatId: Long,
        messageId: Long,
        text: String,
        keyboard: TelegramKeyboard.Inline?,
    ): CompletableFuture<TelegramRequestResult> =
        operation({ TdlightMessageMapper.response(chatId, TelegramResponse(text, keyboard?.rows), messageId) })

    override fun answerCallback(id: String): CompletableFuture<TelegramRequestResult> =
        operation({ TdlightMessageMapper.acknowledgement(id) })
}
