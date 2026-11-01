package random.telegramhomebot.integrations.telegram.mtproto

import dev.telegram.api.OutgoingTelegramMessage
import dev.telegram.api.TelegramKeyboard
import dev.telegram.api.TelegramRequestFailedException
import dev.telegram.api.TelegramRequestRejectedException
import dev.telegram.api.TelegramRequestResult
import dev.telegram.tdlight.TdlightClient
import dev.telegram.tdlight.TdlightConnectionState
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.springframework.boot.actuate.health.Status
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class TdlightAdapterTest {
    @Test fun `legacy future preserves rejected native error and ambiguous timeout outcomes`() {
        val core = mock(TdlightClient::class.java)
        val adapter = TdlightTransport({ core }, { emptyList() }, 3)
        for (error in listOf(TelegramRequestRejectedException(), TelegramRequestFailedException(400), TelegramRequestFailedException())) {
            val request = CompletableFuture<TelegramRequestResult>()
            `when`(core.sendToDefault("notification", null)).thenReturn(request)
            val result = adapter.sendMessage("notification")
            request.completeExceptionally(error)
            assertSame(error, assertThrows(java.util.concurrent.CompletionException::class.java) { result.join() }.cause)
        }
    }

    @Test fun `host retains default and explicit command keyboards and lifecycle health mapping`() {
        val core = mock(TdlightClient::class.java)
        val keyboard = TelegramKeyboard.Reply(listOf(listOf("Wake", "Weather"), listOf("Menu")))
        `when`(core.sendToDefault("notification", keyboard)).thenReturn(CompletableFuture.completedFuture(TelegramRequestResult()))
        val adapter = TdlightTransport({ core }, { listOf("Wake", "Weather", "Menu") }, 2)
        adapter.sendMessage("notification").join()
        verify(core).sendToDefault("notification", keyboard)
        adapter.sendMessage("reply", 42, 77)
        verify(core).send(OutgoingTelegramMessage(42, "reply", 77, keyboard))
        for ((state, health) in listOf(
            TdlightConnectionState.STOPPED to Status.DOWN,
            TdlightConnectionState.INITIALIZING to Status.OUT_OF_SERVICE,
            TdlightConnectionState.DISCONNECTED to Status.OUT_OF_SERVICE,
            TdlightConnectionState.READY to Status.UP,
            TdlightConnectionState.FAILED to Status.DOWN,
        )) {
            `when`(core.state()).thenReturn(state)
            assertEquals(health, adapter.health().status)
        }
        adapter.start()
        verify(core).start()
        adapter.stop()
        verify(core).stop()
    }

    @Test fun `fatal host policy requests exit one and retains independent 55 second watchdog`() {
        val exited = CountDownLatch(1)
        val halted = CountDownLatch(1)
        TdlightFatalShutdown.exit(
            exitProcess = {
                assertEquals(1, it)
                exited.countDown()
            },
            halt = {
                assertEquals(1, it)
                halted.countDown()
            },
            wait = { assertEquals(55_000L, it) },
        )
        assertTrue(exited.await(3, TimeUnit.SECONDS))
        assertTrue(halted.await(3, TimeUnit.SECONDS))
    }

    @Test fun `legacy future cancellation reaches the core queue`() {
        val core = mock(TdlightClient::class.java)
        val pending = CompletableFuture<TelegramRequestResult>()
        `when`(core.sendToDefault("queued", null)).thenReturn(pending)
        val adapter = TdlightTransport({ core }, { emptyList() }, 3)
        assertTrue(adapter.sendMessage("queued").cancel(false))
        assertTrue(pending.isCancelled)
    }
}
