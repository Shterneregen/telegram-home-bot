package random.telegramhomebot.integrations.telegram.mtproto

import it.tdlight.ClientFactory
import it.tdlight.Init
import it.tdlight.TelegramClient
import it.tdlight.client.TelegramError
import it.tdlight.jni.TdApi
import org.springframework.beans.factory.DisposableBean
import org.springframework.beans.factory.ObjectProvider
import org.springframework.boot.actuate.health.Health
import org.springframework.boot.actuate.health.HealthIndicator
import org.springframework.context.SmartLifecycle
import random.telegramhomebot.integrations.telegram.BotProperties
import random.telegramhomebot.integrations.telegram.TelegramEvent
import random.telegramhomebot.integrations.telegram.TelegramResponse
import random.telegramhomebot.integrations.telegram.consume.TelegramMessageHandler
import random.telegramhomebot.integrations.telegram.send.TelegramMessageSender
import random.telegramhomebot.utils.logger
import java.nio.channels.FileChannel
import java.nio.channels.FileLock
import java.nio.file.Files
import java.nio.file.StandardOpenOption
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.CompletableFuture
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.CompletionException
import java.util.concurrent.ScheduledThreadPoolExecutor
import java.util.concurrent.ThreadFactory
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import java.util.concurrent.atomic.AtomicBoolean

class TdlightTransport internal constructor(
    private val settings: MtprotoProperties,
    private val bot: BotProperties,
    private val handler: ObjectProvider<TelegramMessageHandler>,
    private val commands: random.telegramhomebot.services.commands.CommandService,
    private val buttonsInRow: Int,
    private val fatalShutdown: () -> Unit,
    private val verifyGuard: (MtprotoProperties) -> String = EgressGuard::verify,
    private val openFactory: () -> ClientFactory = {
        Init.init()
        it.tdlight.Log.setVerbosityLevel(0)
        ClientFactory.create()
    },
    private val requestTimeoutMillis: Long = 60_000,
    private val startupRetryMillis: Long = 5_000,
) : TelegramMessageSender, SmartLifecycle, HealthIndicator, DisposableBean {
    private val log = logger()
    private var factory: ClientFactory? = null
    private var client: TelegramClient? = null
    private var lockChannel: FileChannel? = null
    private var lock: FileLock? = null
    private val failed = AtomicBoolean(false)
    private val started = AtomicBoolean(false)
    private val stopped = AtomicBoolean(false)
    private val closed = CompletableFuture<Void>()
    private val identity = CompletableFuture<Void>()
    private val destination = CompletableFuture<TdApi.Chat>()
    private val outbox = TdlightOutbox({ requireNotNull(client).sendAsync(it) }, requestTimeoutMillis)
    private val retries = ScheduledThreadPoolExecutor(1) { task ->
        Thread(task, "tdlight-startup-retry").apply { isDaemon = true }
    }.apply { removeOnCancelPolicy = true }
    private val control =
        ThreadPoolExecutor(
            1,
            1,
            0,
            TimeUnit.SECONDS,
            ArrayBlockingQueue(1024),
            ThreadFactory { Thread(it, "tdlight-control").apply { isDaemon = true } },
        )
    private val work =
        ThreadPoolExecutor(
            1,
            1,
            0,
            TimeUnit.SECONDS,
            ArrayBlockingQueue(100),
            ThreadFactory { Thread(it, "telegram-commands").apply { isDaemon = true } },
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

    override fun isRunning() = running

    override fun health(): Health =
        when {
            failed.get() -> Health.down().withDetail("transport", "failed").build()
            stopping || !running -> Health.down().withDetail("transport", "stopped").build()
            !authorized || !destination.isDone -> Health.outOfService().withDetail("transport", "initializing").build()
            !connected -> Health.outOfService().withDetail("transport", "disconnected").build()
            else -> Health.up().build()
        }

    @Synchronized
    override fun start() {
        if (running) return
        check(!stopped.get() && started.compareAndSet(false, true)) { "TDLight lifecycle is one-shot; restart the application context" }
        settings.validate()
        proxyAddress = verifyGuard(settings)
        require(bot.token.isNotBlank()) { "TELEGRAM_TOKEN is required" }
        try {
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
            stop()
            throw IllegalStateException("TDLight initialization failed; verify native libraries and session permissions", e)
        }
    }

    private fun <T : TdApi.Object> request(query: TdApi.Function<T>): CompletableFuture<T> =
        requireNotNull(client).sendAsync(query).orTimeout(60, TimeUnit.SECONDS)

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
            is TdApi.UpdateMessageSendFailed -> log.warn("TDLight message delivery failed (code {})", update.error?.code)
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
                deviceModel = "THB"
                systemVersion = "Java 17"
                applicationVersion = "0.1.0"
                databaseEncryptionKey = byteArrayOf()
            }
        ProxyBootstrap { request(it).thenApply<TdApi.Object> { result -> result } }.initialize(
            TdApi.Proxy(proxyAddress, settings.proxyPort, TdApi.ProxyTypeMtproto(settings.proxySecret)),
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
                    outbox.ready()
                    startupRequest(TdApi.GetChat(bot.chatId.toLong()), "configured notification chat") { chat ->
                        if (chat.id != bot.chatId.toLong()) {
                            fail("configured notification chat")
                        } else {
                            destination.complete(chat)
                            outbox.ready(destination = true)
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
                        val delay = minOf(60_000, startupRetryMillis * (1L shl attempt.coerceAtMost(4)))
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
        // ACK receipt without waiting behind slow business operations; access is checked by the handler.
        event.callbackId?.let(::answerCallback)
        try {
            work.execute {
                try {
                    identity.join()
                    if (!stopping && !failed.get()) handler.getObject().consumeAcknowledged(event)
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
        log.error("TDLight terminal failure at {}; stopping application for supervisor restart", stage)
        fatalShutdown()
    }

    @Synchronized
    override fun stop() {
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

    override fun destroy() = stop()

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

    private fun sendRequest(
        query: TdApi.Function<*>,
        configuredDestination: Boolean = false,
    ): CompletableFuture<Void> =
        outbox.submit(query, configuredDestination).also { result ->
            result.whenComplete { _, error ->
                if (error != null) log.warn("TDLight {} failed ({})", query.javaClass.simpleName, error.javaClass.simpleName)
            }
        }

    override fun sendMessage(messageText: String): CompletableFuture<Void> =
        if (messageText.isBlank()) CompletableFuture.completedFuture(null)
        else sendRequest(message(messageText, bot.chatId.toLong(), null), configuredDestination = true)

    override fun sendMessage(
        messageText: String,
        chatId: Long,
        replyToMessageId: Long?,
    ) {
        if (messageText.isBlank()) return
        sendRequest(message(messageText, chatId, replyToMessageId))
    }

    private fun message(
        text: String,
        chatId: Long,
        replyId: Long?,
    ): TdApi.SendMessage {
        val buttons = commands.getAllEnabledCommands().map { it.getButtonName() }.chunked(buttonsInRow.coerceAtLeast(1))
        return TdlightMessageMapper.message(text, chatId, replyId, buttons)
    }

    override fun send(
        chatId: Long,
        response: TelegramResponse,
        editMessageId: Long?,
    ) {
        sendRequest(TdlightMessageMapper.response(chatId, response, editMessageId))
    }

    override fun answerCallback(id: String) {
        sendRequest(TdlightMessageMapper.acknowledgement(id))
    }
}
