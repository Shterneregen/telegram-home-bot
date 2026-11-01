package dev.telegram.tdlight

import dev.telegram.api.OutgoingTelegramMessage
import dev.telegram.api.TelegramDeliveryEvent
import dev.telegram.api.TelegramEvent
import dev.telegram.api.TelegramEventHandler
import dev.telegram.api.TelegramRequestRejectedException
import it.tdlight.ClientFactory
import it.tdlight.ResultHandler
import it.tdlight.TelegramClient
import it.tdlight.jni.TdApi
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.mockito.Mockito.RETURNS_DEFAULTS
import org.mockito.Mockito.doAnswer
import org.mockito.Mockito.mock
import org.mockito.Mockito.timeout
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.`when`
import java.nio.channels.FileChannel
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.time.Instant
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionException
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

class TdlightTransportTest {
    @TempDir lateinit var directory: Path

    private fun <T> any(): T = org.mockito.ArgumentMatchers.any()

    // Native requests and updates are controlled independently to exercise real lifecycle/queue code.
    private inner class Fixture(
        requestTimeoutMillis: Long = 60_000,
        val defaultChatId: Long? = 42,
        autoStart: Boolean = true,
        initialization: () -> Unit = {},
        eventCapacity: Int = 100,
        controlCapacity: Int = 1024,
        acknowledgementCapacity: Int = 16,
        outgoingCapacity: Int = 100,
        acknowledgement: CompletableFuture<TdApi.Ok> = CompletableFuture.completedFuture(TdApi.Ok()),
        failureListener: () -> Unit = {},
        deliveryListener: () -> Unit = {},
        address: String = "192.0.2.1",
    ) : AutoCloseable {
        val requests = CopyOnWriteArrayList<TdApi.Function<*>>()
        val me = CompletableFuture<TdApi.User>()
        var chat = CompletableFuture<TdApi.Chat>()
        val failures = AtomicInteger()
        val deliveries = CopyOnWriteArrayList<TelegramDeliveryEvent>()
        val handler = mock(TelegramEventHandler::class.java)
        lateinit var updates: ResultHandler<TdApi.Update>
        private lateinit var proxy: TdApi.AddedProxy
        val client =
            mock(TelegramClient::class.java) { call ->
                when (call.method.name) {
                    "initialize" -> {
                        updates = call.getArgument(0)
                        initialization()
                        null
                    }
                    "sendAsync" -> {
                        val query = call.getArgument<TdApi.Function<*>>(0)
                        requests.add(query)
                        when (query) {
                            is TdApi.GetMe -> me
                            is TdApi.GetChat -> chat
                            is TdApi.AnswerCallbackQuery -> acknowledgement
                            else ->
                                CompletableFuture.completedFuture(
                                    when (query) {
                                        is TdApi.AddProxy ->
                                            TdApi.AddedProxy().apply {
                                                id = 1
                                                isEnabled = true
                                                proxy = query.proxy
                                            }.also { proxy = it }
                                        is TdApi.GetProxies -> TdApi.AddedProxies(arrayOf(proxy))
                                        is TdApi.SendMessage -> TdApi.Message()
                                        else -> TdApi.Ok()
                                    },
                                )
                        }
                    }
                    "send" -> {
                        if (call.getArgument<Any>(0) is TdApi.Close) emit(TdApi.UpdateAuthorizationState(TdApi.AuthorizationStateClosed()))
                        null
                    }
                    else -> RETURNS_DEFAULTS.answer(call)
                }
            }
        val factory = mock(ClientFactory::class.java).also { `when`(it.createClient()).thenReturn(client) }

        val transport =
            TdlightClient(
                TdlightClientConfig(
                    12345,
                    "a".repeat(32),
                    directory,
                    MtprotoProxyConfig("proxy.example.invalid", secret = "ee" + "a".repeat(32) + "6578616d706c652e636f6d"),
                    defaultChatId,
                    TdlightTuning(
                        requestTimeoutMillis = requestTimeoutMillis,
                        startupRetryMillis = 10,
                        eventCapacity = eventCapacity,
                        controlCapacity = controlCapacity,
                        acknowledgementCapacity = acknowledgementCapacity,
                        outgoingCapacity = outgoingCapacity,
                    ),
                ),
                TdlightBotCredentials("123:test"),
                handler,
                NetworkEgressVerifier { VerifiedProxyEndpoint(it, address) },
                onFailure = {
                    failures.incrementAndGet()
                    failureListener()
                },
                onDelivery = {
                    deliveries.add(it)
                    deliveryListener()
                },
                openFactory = { factory },
            )

        init {
            if (autoStart) transport.start()
        }

        fun emit(update: TdApi.Update) = updates.onResult(update)

        fun authorize() {
            emit(TdApi.UpdateAuthorizationState(TdApi.AuthorizationStateWaitTdlibParameters()))
            emit(TdApi.UpdateAuthorizationState(TdApi.AuthorizationStateReady()))
            await { requests.any { it is TdApi.GetMe } }
        }

        fun identify() {
            me.complete(
                TdApi.User().apply {
                    id = 123
                    type = TdApi.UserTypeBot()
                },
            )
            if (defaultChatId != null) await { requests.any { it is TdApi.GetChat } }
        }

        fun ready() {
            authorize()
            identify()
            chat.complete(TdApi.Chat().apply { id = 42 })
            emit(TdApi.UpdateConnectionState(TdApi.ConnectionStateReady()))
            await { transport.state() == TdlightConnectionState.READY }
        }

        override fun close() = transport.stop()
    }

    private fun message(id: Long) =
        TdApi.UpdateNewMessage(
            TdApi.Message().apply {
                this.id = id
                chatId = 42
                senderId = TdApi.MessageSenderUser(42)
                date = Instant.now().epochSecond.toInt()
                content = TdApi.MessageText().apply { text = TdApi.FormattedText("/test", emptyArray()) }
            },
        )

    private fun await(condition: () -> Boolean) {
        val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5)
        while (!condition() && System.nanoTime() < deadline) Thread.sleep(10)
        assertTrue(condition(), "Condition was not reached within 5 seconds")
    }

    @Test fun `full ACK queue drops callback while admitted ACK and text do not wait for native response`() {
        val acknowledgement = CompletableFuture<TdApi.Ok>()
        Fixture(acknowledgementCapacity = 1, acknowledgement = acknowledgement).use { f ->
            f.ready()
            val handled = CopyOnWriteArrayList<TelegramEvent>()
            doAnswer { call ->
                handled.add(call.getArgument(0))
                null
            }.`when`(f.handler).handle(any())
            for (callbackId in listOf(76L, 77L)) {
                f.emit(
                    TdApi.UpdateNewCallbackQuery().apply {
                        id = callbackId
                        senderUserId = 42
                        chatId = 42
                        messageId = 10
                        payload = TdApi.CallbackQueryPayloadData("/menu".toByteArray())
                    },
                )
            }
            // The text event is a barrier through both serial queues, without completing the first ACK.
            f.emit(message(20))
            await { handled.any { it.messageId == 20L } }
            assertEquals(listOf("76", null), handled.map { it.callbackId })
            assertEquals(listOf(76L), f.requests.filterIsInstance<TdApi.AnswerCallbackQuery>().map { it.callbackQueryId })
            assertFalse(acknowledgement.isDone)
            acknowledgement.complete(TdApi.Ok())
            f.emit(message(21))
            await { handled.any { it.messageId == 21L } }
            assertEquals(listOf("76", null, null), handled.map { it.callbackId })
            assertEquals(0, f.failures.get())
        }
    }

    @Test fun `blank send and edit reject before admission both during startup and when ready`() {
        for (ready in listOf(false, true)) {
            Fixture(outgoingCapacity = 1).use { f ->
                if (ready) f.ready()
                for (text in listOf("", " \t\n")) {
                    val results =
                        listOf(
                            f.transport.send(OutgoingTelegramMessage(42, text)),
                            f.transport.edit(42, 10, text, null),
                        )
                    results.forEach { result ->
                        assertTrue(result.isCompletedExceptionally)
                        val error = assertThrows(CompletionException::class.java) { result.join() }
                        assertTrue(error.cause is TelegramRequestRejectedException)
                    }
                }
                assertFalse(f.requests.any { it is TdApi.SendMessage || it is TdApi.EditMessageText })
                // All invalid operations must leave the single outgoing slot available.
                val valid = f.transport.send(OutgoingTelegramMessage(42, "valid"))
                if (!ready) {
                    assertFalse(valid.isDone)
                    f.ready()
                }
                valid.get(3, TimeUnit.SECONDS)
                assertEquals(1, f.requests.count { it is TdApi.SendMessage })
                assertFalse(f.requests.any { it is TdApi.EditMessageText })
            }
        }
    }

    @Test fun `startup updates wait for bot identity and notifications wait for configured chat`() {
        Fixture().use { f ->
            f.transport.sendToDefault("notification")
            f.authorize()
            f.emit(message(10))
            verifyNoInteractions(f.handler)
            assertFalse(f.requests.any { it is TdApi.SendMessage })
            f.identify()
            verify(f.handler, timeout(3000)).handle(any())
            assertFalse(f.requests.any { it is TdApi.SendMessage })
            assertEquals(42L, (f.requests.single { it is TdApi.GetChat } as TdApi.GetChat).chatId)
            f.chat.complete(TdApi.Chat().apply { id = 42 })
            await { f.requests.any { it is TdApi.SendMessage } }
            assertEquals(42L, (f.requests.single { it is TdApi.SendMessage } as TdApi.SendMessage).chatId)
        }
    }

    @Test fun `disconnect does not drop queued commands or reject replies and reconnect restores health`() {
        Fixture().use { f ->
            f.ready()
            val entered = CountDownLatch(1)
            val release = CountDownLatch(1)
            doAnswer { call ->
                if (call.getArgument<TelegramEvent>(0).messageId == 1L) {
                    entered.countDown()
                    check(release.await(5, TimeUnit.SECONDS))
                }
                null
            }.`when`(f.handler).handle(any())
            try {
                f.emit(message(1))
                assertTrue(entered.await(3, TimeUnit.SECONDS))
                f.emit(message(2))
                f.emit(TdApi.UpdateConnectionState(TdApi.ConnectionStateConnectingToProxy()))
                await { f.transport.state() == TdlightConnectionState.DISCONNECTED }
                f.transport.send(OutgoingTelegramMessage(42, "reply", 2))
                await { f.requests.any { it is TdApi.SendMessage } }
            } finally {
                release.countDown()
            }
            verify(f.handler, timeout(3000).times(2)).handle(any())
            f.emit(TdApi.UpdateConnectionState(TdApi.ConnectionStateReady()))
            await { f.transport.state() == TdlightConnectionState.READY }
            assertEquals(0, f.failures.get())
            assertEquals(1, f.requests.count { it is TdApi.SendMessage })
        }
    }

    @Test fun `callback receipt is acknowledged while command worker is busy`() {
        Fixture().use { f ->
            f.ready()
            val entered = CountDownLatch(1)
            val release = CountDownLatch(1)
            doAnswer {
                entered.countDown()
                release.await(5, TimeUnit.SECONDS)
                null
            }
                .`when`(f.handler).handle(any())
            try {
                f.emit(message(1))
                assertTrue(entered.await(3, TimeUnit.SECONDS))
                f.emit(
                    TdApi.UpdateNewCallbackQuery().apply {
                        id = 77
                        senderUserId = 42
                        chatId = 42
                        messageId = 10
                        payload = TdApi.CallbackQueryPayloadData("/menu".toByteArray())
                    },
                )
                await { f.requests.any { it is TdApi.AnswerCallbackQuery } }
                assertEquals(77L, (f.requests.single { it is TdApi.AnswerCallbackQuery } as TdApi.AnswerCallbackQuery).callbackQueryId)
            } finally {
                release.countDown()
            }
        }
    }

    @Test fun `unexpected close is terminal and destruction still releases factory and session lock`() {
        Fixture().use { f ->
            f.ready()
            repeat(2) { f.emit(TdApi.UpdateAuthorizationState(TdApi.AuthorizationStateClosed())) }
            await { f.failures.get() == 1 }
            assertFalse(f.transport.isRunning())
            assertEquals(TdlightConnectionState.FAILED, f.transport.state())
            f.transport.sendToDefault("not sent")
            assertFalse(f.requests.any { it is TdApi.SendMessage })
            f.transport.close()
            verify(f.factory).close()
            FileChannel.open(directory.resolve("session.lock"), StandardOpenOption.WRITE).use { channel ->
                channel.tryLock().use { assertNotNull(it) }
            }
            assertThrows(IllegalStateException::class.java) { f.transport.start() }
        }
    }

    @Test fun `wrong saved bot identity never dispatches commands or resolves destination`() {
        Fixture().use { f ->
            f.authorize()
            f.emit(message(1))
            f.me.complete(
                TdApi.User().apply {
                    id = 999
                    type = TdApi.UserTypeBot()
                },
            )
            await { f.failures.get() == 1 }
            verifyNoInteractions(f.handler)
            assertFalse(f.requests.any { it is TdApi.GetChat || it is TdApi.SendMessage })
        }
    }

    @Test fun `blocked native factory cannot block stop indefinitely or release a live session lock`() {
        Fixture().use { f ->
            f.ready()
            val release = CountDownLatch(1)
            doAnswer {
                release.await(20, TimeUnit.SECONDS)
                null
            }.`when`(f.factory).close()
            val before = System.nanoTime()
            try {
                f.transport.stop()
                assertTrue(System.nanoTime() - before < TimeUnit.SECONDS.toNanos(12))
                FileChannel.open(directory.resolve("session.lock"), StandardOpenOption.WRITE).use { channel ->
                    assertThrows(java.nio.channels.OverlappingFileLockException::class.java) { channel.tryLock() }
                }
            } finally {
                release.countDown()
            }
            await {
                FileChannel.open(directory.resolve("session.lock"), StandardOpenOption.WRITE).use { channel ->
                    try {
                        channel.tryLock()?.use { true } ?: false
                    } catch (_: java.nio.channels.OverlappingFileLockException) {
                        false
                    }
                }
            }
        }
    }

    @Test fun `inaccessible configured chat fails visibly instead of losing notifications`() {
        Fixture().use { f ->
            f.transport.sendToDefault("notification")
            f.authorize()
            f.identify()
            f.chat.completeExceptionally(IllegalStateException("unavailable test chat"))
            await { f.failures.get() == 1 }
            assertEquals(TdlightConnectionState.FAILED, f.transport.state())
            assertFalse(f.requests.any { it is TdApi.SendMessage })
        }
    }

    @Test fun `startup notifications survive the request timeout and retain FIFO order`() {
        Fixture(requestTimeoutMillis = 20).use { f ->
            val first = f.transport.sendToDefault("first")
            val second = f.transport.sendToDefault("second")
            Thread.sleep(100)
            assertFalse(first.isDone)
            assertFalse(second.isDone)
            f.ready()
            first.get(3, TimeUnit.SECONDS)
            second.get(3, TimeUnit.SECONDS)
            val texts =
                f.requests.filterIsInstance<TdApi.SendMessage>().map {
                    (it.inputMessageContent as TdApi.InputMessageText).text.text
                }
            assertEquals(listOf("first", "second"), texts)
        }
    }

    @Test fun `pending GetChat survives outage without duplicate requests or fatal shutdown`() {
        Fixture(requestTimeoutMillis = 20).use { f ->
            f.authorize()
            f.identify()
            f.emit(TdApi.UpdateConnectionState(TdApi.ConnectionStateConnectingToProxy()))
            Thread.sleep(100)
            assertEquals(TdlightConnectionState.INITIALIZING, f.transport.state())
            assertEquals(0, f.failures.get())
            assertEquals(1, f.requests.count { it is TdApi.GetChat })
            f.chat.complete(TdApi.Chat().apply { id = 42 })
            f.emit(TdApi.UpdateConnectionState(TdApi.ConnectionStateReady()))
            await { f.transport.state() == TdlightConnectionState.READY }
        }
    }

    @Test fun `transient GetChat error retries but permanent access error is terminal`() {
        Fixture().use { f ->
            f.authorize()
            f.identify()
            val previous = f.chat
            f.chat = CompletableFuture()
            previous.completeExceptionally(java.util.concurrent.TimeoutException())
            await { f.requests.count { it is TdApi.GetChat } == 2 }
            assertEquals(0, f.failures.get())
            assertEquals(TdlightConnectionState.INITIALIZING, f.transport.state())
            f.chat.completeExceptionally(it.tdlight.client.TelegramError(TdApi.Error(400, "inaccessible")))
            await { f.failures.get() == 1 }
        }
    }

    @Test fun `stop rejects pending notifications and prevents later gate completion from sending`() {
        Fixture().use { f ->
            val pending = f.transport.sendToDefault("notification")
            f.authorize()
            f.identify()
            f.transport.stop()
            assertThrows(java.util.concurrent.ExecutionException::class.java) { pending.get(3, TimeUnit.SECONDS) }
            f.chat.complete(TdApi.Chat().apply { id = 42 })
            assertFalse(f.requests.any { it is TdApi.SendMessage })
        }
    }

    @Test fun `temporary server error recovers notification delivery without restart`() {
        Fixture().use { f ->
            val notification = f.transport.sendToDefault("notification")
            f.authorize()
            f.identify()
            val previous = f.chat
            f.chat = CompletableFuture()
            previous.completeExceptionally(it.tdlight.client.TelegramError(TdApi.Error(503, "temporary")))
            await { f.requests.count { it is TdApi.GetChat } == 2 }
            f.chat.complete(TdApi.Chat().apply { id = 42 })
            notification.get(3, TimeUnit.SECONDS)
            assertEquals(0, f.failures.get())
            assertEquals(1, f.requests.count { it is TdApi.SendMessage })
        }
    }

    @Test fun `no default chat authenticates receives events and sends explicit messages without implicit keyboard`() {
        Fixture(defaultChatId = null).use { f ->
            f.ready()
            assertFalse(f.requests.any { it is TdApi.GetChat })
            assertTrue(f.transport.sendToDefault("missing").isCompletedExceptionally)
            f.transport.send(OutgoingTelegramMessage(777, "explicit")).get(3, TimeUnit.SECONDS)
            val sent = f.requests.filterIsInstance<TdApi.SendMessage>().single()
            assertEquals(777, sent.chatId)
            assertNull(sent.replyMarkup)
            f.emit(message(15))
            verify(f.handler, timeout(3000)).handle(any())
        }
    }

    @Test fun `verified numeric proxy is configured in native bootstrap`() {
        Fixture().use { f ->
            f.ready()
            val proxy = f.requests.filterIsInstance<TdApi.AddProxy>().single().proxy
            assertEquals("192.0.2.1", proxy.server)
            assertEquals(443, proxy.port)
            assertTrue((proxy.type as TdApi.ProxyTypeMtproto).secret.startsWith("ee"))
        }
    }

    @Test fun `delivery updates remain distinct from request completion`() {
        Fixture().use { f ->
            f.ready()
            f.transport.send(OutgoingTelegramMessage(42, "text")).join()
            assertTrue(f.deliveries.isEmpty())
            f.emit(
                TdApi.UpdateMessageSendSucceeded().apply {
                    oldMessageId = -1
                    message =
                        TdApi.Message().apply {
                            chatId = 42
                            id = 99
                        }
                },
            )
            f.emit(
                TdApi.UpdateMessageSendFailed().apply {
                    oldMessageId = -2
                    message = TdApi.Message().apply { chatId = 42 }
                    error = TdApi.Error(400, "sensitive native detail")
                },
            )
            await { f.deliveries.size == 2 }
            assertEquals(TelegramDeliveryEvent.Sent(42, -1, 99), f.deliveries[0])
            assertEquals(TelegramDeliveryEvent.Failed(42, -2, 400), f.deliveries[1])
        }
    }

    @Test fun `verifier failure or identity mismatch prevents factory creation and notifies host once`() {
        for (verifier in listOf(
            NetworkEgressVerifier { error("sensitive verifier detail") },
            NetworkEgressVerifier { VerifiedProxyEndpoint(ProxyEndpoint("other.invalid", 443), "192.0.2.1") },
            *NumericProxyAddressTest.invalidAddresses.map {
                    address ->
                NetworkEgressVerifier { VerifiedProxyEndpoint(it, address) }
            }.toTypedArray(),
        )) {
            val factories = AtomicInteger()
            val failures = AtomicInteger()
            val config =
                TdlightClientConfig(
                    1,
                    "a".repeat(32),
                    directory,
                    MtprotoProxyConfig("proxy.invalid", secret = "ee" + "a".repeat(34)),
                )
            TdlightClient(
                config,
                TdlightBotCredentials("123:test"),
                {},
                verifier,
                { failures.incrementAndGet() },
                {},
                {
                    factories.incrementAndGet()
                    error("must not initialize")
                },
            ).use { client ->
                val pending = client.send(OutgoingTelegramMessage(1, "buffered"))
                val error = assertThrows(IllegalStateException::class.java) { client.start() }
                assertFalse(error.toString().contains("sensitive"))
                assertEquals(TdlightConnectionState.FAILED, client.state())
                assertEquals(0, factories.get())
                assertEquals(1, failures.get())
                assertThrows(java.util.concurrent.ExecutionException::class.java) { pending.get(3, TimeUnit.SECONDS) }
                assertThrows(IllegalStateException::class.java) { client.start() }
            }
        }
    }

    @Test fun `concurrent start and close remain idempotent`() {
        Fixture().use { f ->
            f.ready()
            val starts = (1..8).map { CompletableFuture.runAsync { f.transport.start() } }
            CompletableFuture.allOf(*starts.toTypedArray()).get(3, TimeUnit.SECONDS)
            verify(f.factory, times(1)).createClient()
            val stops = (1..8).map { CompletableFuture.runAsync { f.transport.close() } }
            CompletableFuture.allOf(*stops.toTypedArray()).get(3, TimeUnit.SECONDS)
            verify(f.factory, times(1)).close()
            assertEquals(TdlightConnectionState.STOPPED, f.transport.state())
        }
    }

    @Test fun `terminal failure and stop do not wait for dependent request continuations`() {
        for (terminal in listOf(false, true)) {
            Fixture().use { f ->
                val first = f.transport.sendToDefault("first")
                val second = f.transport.sendToDefault("second")
                val entered = CountDownLatch(1)
                val release = CountDownLatch(1)
                val observer =
                    first.handle { _, _ ->
                        entered.countDown()
                        assertThrows(java.util.concurrent.ExecutionException::class.java) { second.get(3, TimeUnit.SECONDS) }
                        check(release.await(5, TimeUnit.SECONDS))
                    }
                try {
                    if (terminal) {
                        f.emit(TdApi.UpdateAuthorizationState(TdApi.AuthorizationStateClosed()))
                        await { f.failures.get() == 1 }
                    } else {
                        CompletableFuture.runAsync { f.transport.stop() }.get(3, TimeUnit.SECONDS)
                    }
                    assertTrue(entered.await(3, TimeUnit.SECONDS))
                    CompletableFuture.runAsync { f.transport.stop() }.get(3, TimeUnit.SECONDS)
                    verify(f.factory, times(1)).close()
                    FileChannel.open(directory.resolve("session.lock"), StandardOpenOption.WRITE).use { channel ->
                        channel.tryLock().use { assertNotNull(it) }
                    }
                    assertFalse(observer.isDone)
                } finally {
                    release.countDown()
                }
                observer.get(3, TimeUnit.SECONDS)
            }
        }
    }

    @Test fun `close racing initialization waits for ownership and releases it once`() {
        val entered = CountDownLatch(1)
        val release = CountDownLatch(1)
        Fixture(autoStart = false, initialization = {
            entered.countDown()
            check(release.await(5, TimeUnit.SECONDS))
        }).use { f ->
            val start = CompletableFuture.runAsync { f.transport.start() }
            assertTrue(entered.await(3, TimeUnit.SECONDS))
            val stopEntered = CountDownLatch(1)
            val close =
                CompletableFuture.runAsync {
                    stopEntered.countDown()
                    f.transport.close()
                }
            try {
                assertTrue(stopEntered.await(3, TimeUnit.SECONDS))
                assertFalse(close.isDone)
            } finally {
                release.countDown()
            }
            start.get(3, TimeUnit.SECONDS)
            close.get(3, TimeUnit.SECONDS)
            verify(f.factory, times(1)).createClient()
            verify(f.factory, times(1)).close()
            assertEquals(TdlightConnectionState.STOPPED, f.transport.state())
        }
    }

    @Test fun `throwing failure and delivery listeners preserve cleanup and control processing`() {
        Fixture(failureListener = { error("host failure") }, deliveryListener = { error("host delivery") }).use { f ->
            f.ready()
            f.emit(
                TdApi.UpdateMessageSendSucceeded().apply {
                    message =
                        TdApi.Message().apply {
                            chatId = 42
                            id = 1
                        }
                },
            )
            f.emit(TdApi.UpdateConnectionState(TdApi.ConnectionStateConnecting()))
            await { f.transport.state() == TdlightConnectionState.DISCONNECTED }
            assertEquals(1, f.deliveries.size)
            f.emit(TdApi.UpdateAuthorizationState(TdApi.AuthorizationStateClosed()))
            await { f.failures.get() == 1 }
            f.transport.close()
            verify(f.factory, times(1)).close()
        }
    }

    @Test fun `full event queue still acknowledges callback and never replays dropped action`() {
        Fixture(eventCapacity = 1).use { f ->
            f.ready()
            val entered = CountDownLatch(1)
            val release = CountDownLatch(1)
            val handled = CopyOnWriteArrayList<Long>()
            doAnswer { call ->
                val event = call.getArgument<TelegramEvent>(0)
                handled.add(event.messageId)
                if (event.messageId == 1L) {
                    entered.countDown()
                    check(release.await(5, TimeUnit.SECONDS))
                }
                null
            }.`when`(f.handler).handle(any())
            try {
                f.emit(message(1))
                assertTrue(entered.await(3, TimeUnit.SECONDS))
                f.emit(message(2))
                f.emit(
                    TdApi.UpdateNewCallbackQuery().apply {
                        id = 77
                        senderUserId = 42
                        chatId = 42
                        messageId = 3
                        payload = TdApi.CallbackQueryPayloadData("/menu".toByteArray())
                    },
                )
                // This state update is processed after dispatch has attempted the full queue.
                f.emit(TdApi.UpdateConnectionState(TdApi.ConnectionStateConnecting()))
                await { f.transport.state() == TdlightConnectionState.DISCONNECTED }
                assertEquals(1, f.requests.count { it is TdApi.AnswerCallbackQuery })
            } finally {
                release.countDown()
            }
            verify(f.handler, timeout(3000).times(2)).handle(any())
            assertEquals(listOf(1L, 2L), handled)
        }
    }

    @Test fun `control queue overflow reports one terminal failure while listener is blocked`() {
        val entered = CountDownLatch(1)
        val release = CountDownLatch(1)
        Fixture(controlCapacity = 4, deliveryListener = {
            entered.countDown()
            // Cleanup interrupts the control worker; still require an explicit release here.
            while (release.count > 0) {
                try {
                    release.await()
                } catch (_: InterruptedException) {
                }
            }
        }).use { f ->
            f.ready()
            try {
                f.emit(TdApi.UpdateMessageSendSucceeded().apply { message = TdApi.Message() })
                assertTrue(entered.await(3, TimeUnit.SECONDS))
                f.emit(TdApi.UpdateConnectionState(TdApi.ConnectionStateConnecting()))
                repeat(5) { f.emit(message(7L + it)) }
                await { f.failures.get() == 1 }
                assertEquals(TdlightConnectionState.FAILED, f.transport.state())
                f.transport.close()
                verify(f.factory, times(1)).close()
                verifyNoInteractions(f.handler)
            } finally {
                release.countDown()
            }
        }
    }

    @Test fun `normalized IPv6 endpoint reaches proxy bootstrap`() {
        Fixture(address = "2001:DB8::1").use { f ->
            f.ready()
            val proxy = f.requests.filterIsInstance<TdApi.AddProxy>().single().proxy
            assertEquals("2001:db8:0:0:0:0:0:1", proxy.server)
            assertEquals(443, proxy.port)
        }
    }
}
