package random.telegramhomebot.auth.services;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class LoginAttemptService {

    private final int maxAttempts;
    private final LoadingCache<String, Integer> attemptsCache;

    public LoginAttemptService(int blockingTimeInMinutes, int maxAttempts) {
        this.maxAttempts = maxAttempts;
        attemptsCache = CacheBuilder.newBuilder()
                .expireAfterWrite(blockingTimeInMinutes, TimeUnit.MINUTES).build(new CacheLoader<>() {
                    public Integer load(String key) {
                        return 0;
                    }
                });
    }

    public void loginSucceeded(String key) {
        attemptsCache.invalidate(key);
    }

    public void loginFailed(String key) {
        int attempts;
        try {
            attempts = attemptsCache.get(key);
        } catch (ExecutionException e) {
            attempts = 0;
        }
        attempts++;
        attemptsCache.put(key, attempts);
    }

    public boolean isBlocked(String key) {
        try {
            return attemptsCache.get(key) >= maxAttempts;
        } catch (ExecutionException e) {
            return false;
        }
    }
}
