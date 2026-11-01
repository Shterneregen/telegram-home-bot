package random.telegramhomebot.integrations.telegram.mtproto

import it.tdlight.jni.TdApi
import java.util.concurrent.CompletableFuture
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.TimeUnit

/** Bounded, process-local FIFO. Only submitted requests have a deadline. */
internal class TdlightOutbox(
    private val send: (TdApi.Function<*>) -> CompletableFuture<out TdApi.Object>,
    private val timeoutMillis: Long = 60_000,
) {
    private class Entry(val query: TdApi.Function<*>, val destination: Boolean, val ack: Boolean) {
        val result = CompletableFuture<Void>()
        var submitted = false
    }

    private val monitor = Any()
    private val messages = ArrayDeque<Entry>()
    private val acknowledgements = ArrayDeque<Entry>()
    private val pending = mutableSetOf<Entry>()
    private var identityReady = false
    private var destinationReady = false
    private var closed = false

    fun submit(query: TdApi.Function<*>, destination: Boolean = false): CompletableFuture<Void> {
        val entry = Entry(query, destination, query is TdApi.AnswerCallbackQuery)
        synchronized(monitor) {
            if (closed || pending.count { it.ack == entry.ack } >= if (entry.ack) 16 else 100) {
                return CompletableFuture.failedFuture(RejectedExecutionException("TDLight outgoing queue unavailable"))
            }
            pending.add(entry)
            (if (entry.ack) acknowledgements else messages).addLast(entry)
        }
        entry.result.whenComplete { _, _ ->
            if (entry.result.isCancelled) {
                synchronized(monitor) {
                    // Cancelling an already submitted operation cannot undo native delivery.
                    if (!entry.submitted) {
                        pending.remove(entry)
                        messages.remove(entry)
                        acknowledgements.remove(entry)
                    }
                }
                drain()
            }
        }
        drain()
        return entry.result
    }

    fun ready(destination: Boolean = false) {
        synchronized(monitor) {
            identityReady = true
            if (destination) destinationReady = true
        }
        drain()
    }

    fun close() {
        val rejected = synchronized(monitor) {
            closed = true
            messages.clear()
            acknowledgements.clear()
            pending.toList().also { pending.clear() }
        }
        // Never invoke caller continuations while holding our state lock.
        rejected.forEach { it.result.completeExceptionally(IllegalStateException("TDLight stopped")) }
    }

    private fun drain() {
        val submitted = synchronized(monitor) {
            val requests = mutableListOf<Pair<Entry, CompletableFuture<out TdApi.Object>>>()
            if (!closed && identityReady) {
                for (queue in listOf(acknowledgements, messages)) {
                    while (queue.isNotEmpty()) {
                        val entry = queue.first()
                        if (entry.result.isCancelled) {
                            queue.removeFirst()
                            pending.remove(entry)
                            continue
                        }
                        if (entry.destination && !destinationReady) break
                        queue.removeFirst()
                        entry.submitted = true
                        // Serialise native submission, not completion: retain FIFO even when
                        // gate completion races a new caller. Native sendAsync is non-blocking.
                        val response = try {
                            send(entry.query)
                        } catch (e: Exception) {
                            CompletableFuture.failedFuture<TdApi.Object>(e)
                        }
                        requests.add(entry to response)
                    }
                }
            }
            requests
        }
        submitted.forEach { (entry, response) ->
            response.whenComplete { _, _ -> synchronized(monitor) { pending.remove(entry) } }
            // Time out a view, not the native future. Keep the capacity reservation
            // until TDLib answers so an outage cannot grow native requests unboundedly.
            response.thenApply { null }.orTimeout(timeoutMillis, TimeUnit.MILLISECONDS).whenComplete { _, error ->
                if (error == null) entry.result.complete(null) else entry.result.completeExceptionally(error)
            }
        }
    }
}
