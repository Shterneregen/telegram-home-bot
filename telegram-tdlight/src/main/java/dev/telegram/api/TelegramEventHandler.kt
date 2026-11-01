package dev.telegram.api

/**
 * Receives events on the bounded business worker after bot identity verification.
 * Callbacks are dropped if their ACK cannot be admitted to the outgoing queue. Admitted ACKs are not
 * awaited before event delivery: a successful native response is not guaranteed and failures are not retried.
 * Authorization and deduplication belong to the application. Events rejected by the full queue are dropped.
 */
fun interface TelegramEventHandler {
    /** Handles one event serially; exceptions are logged without replaying the event. */
    fun handle(event: TelegramEvent)
}
