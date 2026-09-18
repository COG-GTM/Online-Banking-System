package com.userfront.service;

import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.stereotype.Service;

/**
 * Tracks consecutive failed login attempts per username and temporarily blocks
 * further attempts once the threshold is reached.
 */
@Service
public class LoginAttemptService {

    public static final int MAX_ATTEMPTS = 5;
    public static final Duration LOCK_DURATION = Duration.ofMinutes(15);

    private static final class Attempts {
        private final AtomicInteger count = new AtomicInteger();
        private volatile Instant lastFailure = Instant.now();
    }

    private final Map<String, Attempts> attemptsByUser = new ConcurrentHashMap<>();

    public void loginFailed(String username) {
        if (username == null) {
            return;
        }
        Attempts attempts = attemptsByUser.computeIfAbsent(key(username), k -> new Attempts());
        if (expired(attempts)) {
            attempts.count.set(0);
        }
        attempts.count.incrementAndGet();
        attempts.lastFailure = Instant.now();
    }

    public void loginSucceeded(String username) {
        if (username != null) {
            attemptsByUser.remove(key(username));
        }
    }

    public boolean isBlocked(String username) {
        if (username == null) {
            return false;
        }
        Attempts attempts = attemptsByUser.get(key(username));
        if (attempts == null) {
            return false;
        }
        if (expired(attempts)) {
            attemptsByUser.remove(key(username));
            return false;
        }
        return attempts.count.get() >= MAX_ATTEMPTS;
    }

    private boolean expired(Attempts attempts) {
        return attempts.lastFailure.plus(LOCK_DURATION).isBefore(Instant.now());
    }

    private String key(String username) {
        return username.toLowerCase(Locale.ROOT);
    }
}
