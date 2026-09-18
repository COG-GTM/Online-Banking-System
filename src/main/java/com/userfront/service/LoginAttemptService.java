package com.userfront.service;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.stereotype.Service;

@Service
public class LoginAttemptService {

    public static final int MAX_ATTEMPTS = 5;
    public static final long BLOCK_DURATION_SECONDS = 900;

    private static final long ATTEMPT_WINDOW_SECONDS = 900;

    private final Map<String, Attempts> attempts = new ConcurrentHashMap<>();

    public void loginSucceeded(String key) {
        attempts.remove(normalize(key));
    }

    public void loginFailed(String key) {
        String normalized = normalize(key);
        Instant now = Instant.now();
        attempts.compute(normalized, (k, current) -> {
            if (current == null || current.isExpired(now)) {
                return new Attempts(now);
            }
            current.increment(now);
            return current;
        });
        purgeExpired(now);
    }

    public boolean isBlocked(String key) {
        Attempts current = attempts.get(normalize(key));
        if (current == null) {
            return false;
        }
        Instant now = Instant.now();
        if (current.isExpired(now)) {
            attempts.remove(normalize(key));
            return false;
        }
        return current.count.get() >= MAX_ATTEMPTS;
    }

    public long secondsUntilUnblocked(String key) {
        Attempts current = attempts.get(normalize(key));
        if (current == null) {
            return 0;
        }
        long elapsed = Instant.now().getEpochSecond() - current.lastAttempt.getEpochSecond();
        long remaining = BLOCK_DURATION_SECONDS - elapsed;
        return remaining > 0 ? remaining : 0;
    }

    private void purgeExpired(Instant now) {
        attempts.entrySet().removeIf(entry -> entry.getValue().isExpired(now));
    }

    private String normalize(String key) {
        return key == null ? "" : key.toLowerCase();
    }

    private static final class Attempts {

        private final AtomicInteger count = new AtomicInteger(1);
        private volatile Instant lastAttempt;

        private Attempts(Instant firstAttempt) {
            this.lastAttempt = firstAttempt;
        }

        private void increment(Instant at) {
            count.incrementAndGet();
            lastAttempt = at;
        }

        private boolean isExpired(Instant now) {
            long elapsed = now.getEpochSecond() - lastAttempt.getEpochSecond();
            long ttl = count.get() >= MAX_ATTEMPTS ? BLOCK_DURATION_SECONDS : ATTEMPT_WINDOW_SECONDS;
            return elapsed >= ttl;
        }
    }
}
