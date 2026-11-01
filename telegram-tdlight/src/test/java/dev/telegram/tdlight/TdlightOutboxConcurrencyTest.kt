package dev.telegram.tdlight

import dev.telegram.api.TelegramRequestFailedException
import dev.telegram.api.TelegramRequestRejectedException
import it.tdlight.jni.TdApi
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CountDownLatch
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

class TdlightOutboxConcurrencyTest {
    @Test fun `close completes dependent pending requests without joining their callbacks`() {
        val sends = AtomicInteger()
        val outbox =
            TdlightOutbox({
                sends.incrementAndGet()
                CompletableFuture.completedFuture(TdApi.Ok())
            }, capacity = 20)
        val pending = (1..20).map { outbox.submit(TdApi.SendMessage()) }
        val workers = CopyOnWriteArrayList<Thread>()
        val callbacks =
            pending.dropLast(1).mapIndexed { index, future ->
                future.handle { _, _ ->
                    workers.add(Thread.currentThread())
                    assertThrows(ExecutionException::class.java) { pending[index + 1].get(3, TimeUnit.SECONDS) }
                }
            }
        try {
            CompletableFuture.runAsync { outbox.close() }.get(3, TimeUnit.SECONDS)
            CompletableFuture.allOf(*callbacks.toTypedArray()).get(3, TimeUnit.SECONDS)
            pending.forEach {
                assertTrue(
                    assertThrows(ExecutionException::class.java) { it.get(3, TimeUnit.SECONDS) }.cause is TelegramRequestRejectedException,
                )
            }
            assertEquals(0, sends.get())
        } finally {
            pending.forEach { it.cancel(false) }
            outbox.close()
        }
        val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(3)
        while (workers.any { it.isAlive } && System.nanoTime() < deadline) Thread.sleep(10)
        assertTrue(workers.none { it.isAlive }, "Completion workers must terminate after close and returning callbacks")
    }

    @Test fun `blocking success and timeout callbacks do not occupy native or timeout threads`() {
        for (timeout in listOf(false, true)) {
            val native = CompletableFuture<TdApi.Object>()
            val outbox = TdlightOutbox({ native }, timeoutMillis = 100, capacity = 2)
            val first = outbox.submit(TdApi.SendMessage())
            val entered = CountDownLatch(1)
            val release = CountDownLatch(1)
            val observer =
                first.handle { _, _ ->
                    assertTrue(Thread.currentThread().name.startsWith("tdlight-completions-"))
                    entered.countDown()
                    check(release.await(5, TimeUnit.SECONDS))
                }
            try {
                outbox.ready()
                if (!timeout) CompletableFuture.runAsync { native.complete(TdApi.Ok()) }.get(2, TimeUnit.SECONDS)
                assertTrue(entered.await(3, TimeUnit.SECONDS))
                // This unrelated deadline must still execute, even while a timeout observer blocks.
                assertThrows(ExecutionException::class.java) {
                    CompletableFuture<Void>().orTimeout(20, TimeUnit.MILLISECONDS).get(2, TimeUnit.SECONDS)
                }
                CompletableFuture.runAsync { outbox.close() }.get(2, TimeUnit.SECONDS)
                assertFalse(observer.isDone)
            } finally {
                release.countDown()
                outbox.close()
            }
            observer.get(3, TimeUnit.SECONDS)
        }
    }

    @Test fun `slow completion retains admission capacity and ACK has a separate reservation`() {
        val native = CompletableFuture<TdApi.Object>()
        val outbox =
            TdlightOutbox({ if (it is TdApi.AnswerCallbackQuery) CompletableFuture.completedFuture(TdApi.Ok()) else native }, capacity = 1)
        val first = outbox.submit(TdApi.SendMessage())
        val entered = CountDownLatch(1)
        val release = CountDownLatch(1)
        val callback =
            first.thenRun {
                entered.countDown()
                check(release.await(5, TimeUnit.SECONDS))
            }
        try {
            outbox.ready()
            native.complete(TdApi.Ok())
            assertTrue(entered.await(3, TimeUnit.SECONDS))
            assertTrue(outbox.submit(TdApi.SendMessage()).isCompletedExceptionally)
            outbox.submit(TdApi.AnswerCallbackQuery()).get(3, TimeUnit.SECONDS)
        } finally {
            release.countDown()
            outbox.close()
        }
        callback.get(3, TimeUnit.SECONDS)
    }

    @Test fun `cancel during submission and late native response never resend or override cancellation`() {
        val entered = CountDownLatch(1)
        val release = CountDownLatch(1)
        val native = CompletableFuture<TdApi.Object>()
        val sends = AtomicInteger()
        val outbox =
            TdlightOutbox({
                sends.incrementAndGet()
                entered.countDown()
                check(release.await(3, TimeUnit.SECONDS))
                native
            }, capacity = 1)
        val pending = outbox.submit(TdApi.SendMessage())
        val drain = CompletableFuture.runAsync { outbox.ready() }
        try {
            assertTrue(entered.await(3, TimeUnit.SECONDS))
            val cancellation = CompletableFuture.supplyAsync { pending.cancel(false) }
            release.countDown()
            assertTrue(cancellation.get(3, TimeUnit.SECONDS))
            drain.get(3, TimeUnit.SECONDS)
            assertTrue(outbox.submit(TdApi.SendMessage()).isCompletedExceptionally)
            native.complete(TdApi.Ok())
            assertTrue(pending.isCancelled)
            assertEquals(1, sends.get())
        } finally {
            release.countDown()
            outbox.close()
        }
    }

    @Test fun `ACK exhaustion is bounded and stop distinguishes queued from submitted requests`() {
        val native = CompletableFuture<TdApi.Object>()
        val outbox = TdlightOutbox({ native }, ackCapacity = 1)
        val ack = outbox.submit(TdApi.AnswerCallbackQuery())
        assertTrue(outbox.submit(TdApi.AnswerCallbackQuery()).isCompletedExceptionally)
        outbox.ready()
        val queued = outbox.submit(TdApi.SendMessage(), destination = true)
        outbox.close()
        assertTrue(assertThrows(ExecutionException::class.java) { ack.get(3, TimeUnit.SECONDS) }.cause is TelegramRequestFailedException)
        assertTrue(
            assertThrows(ExecutionException::class.java) { queued.get(3, TimeUnit.SECONDS) }.cause is TelegramRequestRejectedException,
        )
        native.complete(TdApi.Ok())
        assertTrue(ack.isCompletedExceptionally)
    }
}
