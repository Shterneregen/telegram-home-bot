package random.telegramhomebot.integrations.telegram.mtproto

import it.tdlight.ClientFactory
import it.tdlight.ResultHandler
import it.tdlight.TelegramClient
import it.tdlight.jni.TdApi
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.mockito.Mockito.*
import org.springframework.boot.actuate.health.Status
import random.telegramhomebot.integrations.telegram.TelegramEvent
import random.telegramhomebot.integrations.telegram.TelegramEventHandler
import java.nio.channels.FileChannel
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.time.Instant
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

class TdlightTransportTest {
    @TempDir lateinit var directory: Path

    private fun <T> any(): T = org.mockito.ArgumentMatchers.any()

    // Native requests and updates are controlled independently to exercise real lifecycle/queue code.
    private inner class Fixture(requestTimeoutMillis: Long = 60_000) : AutoCloseable {
        val requests = CopyOnWriteArrayList<TdApi.Function<*>>()
        val me = CompletableFuture<TdApi.User>()
        var chat = CompletableFuture<TdApi.Chat>()
        val failures = AtomicInteger()
        val handler = mock(TelegramEventHandler::class.java)
        lateinit var updates: ResultHandler<TdApi.Update>
        private lateinit var proxy: TdApi.AddedProxy
        val client =
            mock(TelegramClient::class.java) { call ->
                when (call.method.name) {
                    "initialize" -> {
                        updates = call.getArgument(0)
                        null
                    }
                    "sendAsync" -> {
                        val query = call.getArgument<TdApi.Function<*>>(0)
                        requests.add(query)
                        when (query) {
                            is TdApi.GetMe -> me
                            is TdApi.GetChat -> chat
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
            TdlightTransport(
                MtprotoProperties().apply {
                    apiId = 12345
                    apiHash = "a".repeat(32)
                    proxyHost = "proxy.example.invalid"
                    proxySecret = "ee" + "a".repeat(32) + "6578616d706c652e636f6d"
                    dataDir = directory
                },
                TdlightBotCredentials("123:test", 42),
                handler,
                { emptyList() },
                3,
                fatalShutdown = { failures.incrementAndGet() },
                verifyGuard = { "192.0.2.1" },
                openFactory = { factory },
                requestTimeoutMillis = requestTimeoutMillis,
                startupRetryMillis = 10,
            )

        init {
            transport.start()
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
            await { requests.any { it is TdApi.GetChat } }
        }

        fun ready() {
            authorize()
            identify()
            chat.complete(TdApi.Chat().apply { id = 42 })
            emit(TdApi.UpdateConnectionState(TdApi.ConnectionStateReady()))
            await { transport.health().status == Status.UP }
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

    @Test fun `startup updates wait for bot identity and notifications wait for configured chat`() {
        Fixture().use { f ->
            f.transport.sendMessage("notification")
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
                await { f.transport.health().status == Status.OUT_OF_SERVICE }
                f.transport.sendMessage("reply", 42, 2)
                await { f.requests.any { it is TdApi.SendMessage } }
            } finally {
                release.countDown()
            }
            verify(f.handler, timeout(3000).times(2)).handle(any())
            f.emit(TdApi.UpdateConnectionState(TdApi.ConnectionStateReady()))
            await { f.transport.health().status == Status.UP }
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
            assertFalse(f.transport.isRunning)
            assertEquals(Status.DOWN, f.transport.health().status)
            f.transport.sendMessage("not sent")
            assertFalse(f.requests.any { it is TdApi.SendMessage })
            f.transport.destroy()
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
            f.transport.sendMessage("notification")
            f.authorize()
            f.identify()
            f.chat.completeExceptionally(IllegalStateException("unavailable test chat"))
            await { f.failures.get() == 1 }
            assertEquals(Status.DOWN, f.transport.health().status)
            assertFalse(f.requests.any { it is TdApi.SendMessage })
        }
    }

    @Test fun `startup notifications survive the request timeout and retain FIFO order`() {
        Fixture(requestTimeoutMillis = 20).use { f ->
            val first = f.transport.sendMessage("first")
            val second = f.transport.sendMessage("second")
            Thread.sleep(100)
            assertFalse(first.isDone)
            assertFalse(second.isDone)
            f.ready()
            first.get(3, TimeUnit.SECONDS)
            second.get(3, TimeUnit.SECONDS)
            val texts = f.requests.filterIsInstance<TdApi.SendMessage>().map {
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
            assertEquals(Status.OUT_OF_SERVICE, f.transport.health().status)
            assertEquals(0, f.failures.get())
            assertEquals(1, f.requests.count { it is TdApi.GetChat })
            f.chat.complete(TdApi.Chat().apply { id = 42 })
            f.emit(TdApi.UpdateConnectionState(TdApi.ConnectionStateReady()))
            await { f.transport.health().status == Status.UP }
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
            assertEquals(Status.OUT_OF_SERVICE, f.transport.health().status)
            f.chat.completeExceptionally(it.tdlight.client.TelegramError(TdApi.Error(400, "inaccessible")))
            await { f.failures.get() == 1 }
        }
    }

    @Test fun `stop rejects pending notifications and prevents later gate completion from sending`() {
        Fixture().use { f ->
            val pending = f.transport.sendMessage("notification")
            f.authorize()
            f.identify()
            f.transport.stop()
            assertTrue(pending.isCompletedExceptionally)
            f.chat.complete(TdApi.Chat().apply { id = 42 })
            assertFalse(f.requests.any { it is TdApi.SendMessage })
        }
    }

    @Test fun `temporary server error recovers notification delivery without restart`() {
        Fixture().use { f ->
            val notification = f.transport.sendMessage("notification")
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
}
