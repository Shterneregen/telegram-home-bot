package dev.telegram.tdlight

import dev.telegram.api.TelegramRequestFailedException
import dev.telegram.api.TelegramRequestRejectedException
import dev.telegram.api.TelegramRequestResult
import it.tdlight.client.TelegramError
import it.tdlight.jni.TdApi
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionException
import java.util.concurrent.ForkJoinPool
import java.util.concurrent.TimeUnit

/** Bounded FIFO, including outstanding completion callbacks. Only submitted requests have a deadline. */
internal class TdlightOutbox(
    private val send: (TdApi.Function<*>) -> CompletableFuture<out TdApi.Object>,
    private val timeoutMillis: Long = 60_000,
    private val capacity: Int = 100,
    private val ackCapacity: Int = 16,
) {
    private class Entry(val query: TdApi.Function<*>, val destination: Boolean, val ack: Boolean) {
        val result = CompletableFuture<TelegramRequestResult>()
        var submitted = false
        var nativeFinished = false
        var notificationScheduled = false
        var notificationFinished = false
    }

    private val monitor = Any()
    private val messages = ArrayDeque<Entry>()
    private val acknowledgements = ArrayDeque<Entry>()
    private val pending = mutableSetOf<Entry>()
    private var identityReady = false
    private var destinationReady = false
    private var closed = false

    // CompletableFuture.join/get cooperates with ForkJoinPool's managed blocking. A continuation
    // waiting for another admitted request must not prevent that request's completion from running.
    // Admission retains a slot until notification returns, bounding both tasks and compensation.
    private val completions =
        ForkJoinPool(
            2,
            { pool -> ForkJoinPool.defaultForkJoinWorkerThreadFactory.newThread(pool).apply { name = "tdlight-completions-$poolIndex" } },
            null,
            true,
            2,
            minOf(32767L, capacity.toLong() + ackCapacity + 2).toInt(),
            1,
            null,
            1,
            TimeUnit.SECONDS,
        )

    fun submit(
        query: TdApi.Function<*>,
        destination: Boolean = false,
    ): CompletableFuture<TelegramRequestResult> {
        val entry = Entry(query, destination, query is TdApi.AnswerCallbackQuery)
        synchronized(monitor) {
            if (closed || pending.count { it.ack == entry.ack } >= if (entry.ack) ackCapacity else capacity) {
                return CompletableFuture.failedFuture(TelegramRequestRejectedException())
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
        synchronized(monitor) {
            if (closed) return
            closed = true
            messages.clear()
            acknowledgements.clear()
            pending.forEach {
                publish(
                    it,
                    null,
                    if (it.submitted) TelegramRequestFailedException() else TelegramRequestRejectedException(),
                )
            }
            pending.clear()
            // Do not join user continuations during lifecycle cleanup. Already accepted outcomes
            // still run; daemon workers terminate when callbacks return. Never discard queued tasks.
            completions.shutdown()
        }
    }

    private fun retire(entry: Entry) {
        if (entry.nativeFinished && entry.notificationFinished) pending.remove(entry)
    }

    private fun publish(
        entry: Entry,
        result: TelegramRequestResult?,
        error: Throwable?,
    ) {
        synchronized(monitor) {
            if (entry.notificationScheduled) return
            entry.notificationScheduled = true
            completions.execute {
                try {
                    if (error == null) entry.result.complete(result) else entry.result.completeExceptionally(error)
                } finally {
                    synchronized(monitor) {
                        entry.notificationFinished = true
                        retire(entry)
                    }
                }
            }
        }
    }

    private fun drain() {
        val submitted =
            synchronized(monitor) {
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
                            val response =
                                try {
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
            val observed =
                response.whenComplete { _, _ ->
                    synchronized(monitor) {
                        entry.nativeFinished = true
                        retire(entry)
                    }
                }
            // Time out a view, not the native future. Keep the capacity reservation
            // until TDLib answers so an outage cannot grow native requests unboundedly.
            observed.thenApply { value ->
                if (value is TdApi.Message) TelegramRequestResult(value.id, value.chatId) else TelegramRequestResult()
            }.orTimeout(timeoutMillis, TimeUnit.MILLISECONDS).whenComplete { result, error ->
                if (error == null) {
                    publish(entry, result, null)
                } else {
                    val cause = if (error is CompletionException) error.cause else error
                    publish(entry, null, TelegramRequestFailedException((cause as? TelegramError)?.errorCode))
                }
            }
        }
    }
}
