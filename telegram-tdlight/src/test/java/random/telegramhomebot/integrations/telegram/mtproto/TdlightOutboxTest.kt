package random.telegramhomebot.integrations.telegram.mtproto

import it.tdlight.jni.TdApi
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CountDownLatch
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.TimeUnit

class TdlightOutboxTest {
    @Test fun `pre-submit cancellation frees capacity and never sends cancelled notifications`() {
        val submitted = mutableListOf<TdApi.Function<*>>()
        val outbox = TdlightOutbox({ submitted.add(it); CompletableFuture.completedFuture(TdApi.Ok()) })
        val notifications = (1..100).map { outbox.submit(TdApi.SendMessage(), destination = true) }
        assertTrue(outbox.submit(TdApi.SendMessage()).isCompletedExceptionally)
        assertTrue(notifications.first().cancel(false))
        val last = outbox.submit(TdApi.SendMessage(), destination = true)
        outbox.ready(destination = true)
        last.get(1, TimeUnit.SECONDS)
        assertEquals(100, submitted.size)
        assertTrue(notifications.first().isCancelled)
    }

    @Test fun `native timeout neither resends nor releases capacity until native response`() {
        val response = CompletableFuture<TdApi.Object>()
        val submitted = CopyOnWriteArrayList<TdApi.Function<*>>()
        val outbox = TdlightOutbox({ submitted.add(it); response }, timeoutMillis = 20)
        outbox.ready(destination = true)
        val pending = (1..100).map { outbox.submit(TdApi.SendMessage()) }
        pending.forEach { assertThrows(Exception::class.java) { it.get(2, TimeUnit.SECONDS) } }
        assertTrue(outbox.submit(TdApi.SendMessage()).isCompletedExceptionally)
        assertEquals(100, submitted.size)
        response.complete(TdApi.Ok())
        outbox.submit(TdApi.SendMessage()).get(1, TimeUnit.SECONDS)
        assertEquals(101, submitted.size)
    }

    @Test fun `ACK has reserved capacity and bypasses unresolved notification destination`() {
        val submitted = mutableListOf<TdApi.Function<*>>()
        val outbox = TdlightOutbox({ submitted.add(it); CompletableFuture.completedFuture(TdApi.Ok()) })
        repeat(100) { outbox.submit(TdApi.SendMessage(), destination = true) }
        outbox.ready()
        outbox.submit(TdApi.AnswerCallbackQuery()).get(1, TimeUnit.SECONDS)
        assertEquals(1, submitted.size)
        assertTrue(submitted.single() is TdApi.AnswerCallbackQuery)
        outbox.close()
    }

    @Test fun `new caller during startup drain cannot overtake queued notifications`() {
        val submitted = CopyOnWriteArrayList<Long>()
        val entered = CountDownLatch(1)
        val release = CountDownLatch(1)
        val outbox = TdlightOutbox({ query ->
            val id = (query as TdApi.SendMessage).chatId
            submitted.add(id)
            if (id == 1L) {
                entered.countDown()
                check(release.await(3, TimeUnit.SECONDS))
            }
            CompletableFuture.completedFuture(TdApi.Ok())
        })
        outbox.submit(TdApi.SendMessage().apply { chatId = 1 }, destination = true)
        outbox.submit(TdApi.SendMessage().apply { chatId = 2 }, destination = true)
        val drain = CompletableFuture.runAsync { outbox.ready(destination = true) }
        try {
            assertTrue(entered.await(3, TimeUnit.SECONDS))
            val next = CompletableFuture.supplyAsync { outbox.submit(TdApi.SendMessage().apply { chatId = 3 }) }
            release.countDown()
            next.get(3, TimeUnit.SECONDS).get(3, TimeUnit.SECONDS)
            drain.get(3, TimeUnit.SECONDS)
            assertEquals(listOf(1L, 2L, 3L), submitted)
        } finally {
            release.countDown()
            outbox.close()
        }
    }
}
