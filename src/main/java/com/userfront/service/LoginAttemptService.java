package com.userfront.service;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Tracks failed authentication attempts per client IP and per username and blocks
 * further login attempts once the configured threshold is reached.
 */
@Service
public class LoginAttemptService {

    private final int maxAttempts;
    private final Duration blockDuration;
    private final Map<String, Attempts> attemptsByKey = new ConcurrentHashMap<>();

    public LoginAttemptService(@Value("${security.login.max-attempts:5}") int maxAttempts,
                               @Value("${security.login.block-duration-minutes:15}") long blockDurationMinutes) {
        this.maxAttempts = maxAttempts;
        this.blockDuration = Duration.ofMinutes(blockDurationMinutes);
    }

    public void loginFailed(String ip, String username) {
        Instant now = Instant.now();
        registerFailure(ipKey(ip), now);
        registerFailure(usernameKey(username), now);
    }

    public void loginSucceeded(String ip, String username) {
        attemptsByKey.remove(ipKey(ip));
        attemptsByKey.remove(usernameKey(username));
    }

    public boolean isBlocked(String ip, String username) {
        return isKeyBlocked(ipKey(ip)) || isKeyBlocked(usernameKey(username));
    }

    /**
     * Seconds remaining before the caller may attempt to log in again, or zero when not blocked.
     */
    public long secondsUntilUnblocked(String ip, String username) {
        return Math.max(secondsUntilUnblocked(ipKey(ip)), secondsUntilUnblocked(usernameKey(username)));
    }

    private void registerFailure(String key, Instant now) {
        if (key == null) {
            return;
        }
        attemptsByKey.compute(key, (ignored, existing) -> {
            if (existing == null || existing.isExpired(now, blockDuration)) {
                return new Attempts(now);
            }
            existing.increment(now);
            return existing;
        });
    }

    private boolean isKeyBlocked(String key) {
        return secondsUntilUnblocked(key) > 0;
    }

    private long secondsUntilUnblocked(String key) {
        if (key == null) {
            return 0;
        }
        Attempts attempts = attemptsByKey.get(key);
        if (attempts == null) {
            return 0;
        }
        Instant now = Instant.now();
        if (attempts.isExpired(now, blockDuration)) {
            attemptsByKey.remove(key, attempts);
            return 0;
        }
        if (attempts.count() < maxAttempts) {
            return 0;
        }
        long remaining = Duration.between(now, attempts.lastFailure().plus(blockDuration)).getSeconds();
        return Math.max(remaining, 1);
    }

    private static String ipKey(String ip) {
        return ip == null || ip.isEmpty() ? null : "ip:" + ip;
    }

    private static String usernameKey(String username) {
        return username == null || username.isEmpty() ? null : "user:" + username.toLowerCase();
    }

    private static final class Attempts {

        private final AtomicInteger count = new AtomicInteger(1);
        private volatile Instant lastFailure;

        private Attempts(Instant lastFailure) {
            this.lastFailure = lastFailure;
        }

        private void increment(Instant now) {
            count.incrementAndGet();
            lastFailure = now;
        }

        private int count() {
            return count.get();
        }

        private Instant lastFailure() {
            return lastFailure;
        }

        private boolean isExpired(Instant now, Duration blockDuration) {
            return lastFailure.plus(blockDuration).isBefore(now);
        }
    }
}
