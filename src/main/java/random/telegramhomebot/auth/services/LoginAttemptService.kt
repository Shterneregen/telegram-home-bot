package random.telegramhomebot.auth.services

import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import com.google.common.cache.LoadingCache
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit

class LoginAttemptService(blockingTimeInMinutes: Int, private val maxAttempts: Int) {

    private val attemptsCache: LoadingCache<String, Int>

    fun loginSucceeded(key: String) = attemptsCache.invalidate(key)

    fun loginFailed(key: String) {
        var attempts: Int = try {
            attemptsCache[key]
        } catch (e: ExecutionException) {
            0
        }
        attempts++
        attemptsCache.put(key, attempts)
    }

    fun isBlocked(key: String): Boolean = try {
        attemptsCache[key] >= maxAttempts
    } catch (e: ExecutionException) {
        false
    }

    init {
        attemptsCache = CacheBuilder.newBuilder()
            .expireAfterWrite(blockingTimeInMinutes.toLong(), TimeUnit.MINUTES)
            .build(object : CacheLoader<String, Int>() {
                override fun load(key: String): Int {
                    return 0
                }
            })
    }
}
