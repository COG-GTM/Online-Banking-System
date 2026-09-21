package com.userfront.config;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Tracks consecutive failed authentication attempts per username and locks
 * further attempts for a cool down period once the threshold is reached.
 */
@Component
public class LoginAttemptService {

    private final Map<String, Attempts> attempts = new ConcurrentHashMap<>();

    @Value("${app.security.login.max-attempts:5}")
    private int maxAttempts;

    @Value("${app.security.login.lock-duration-seconds:900}")
    private long lockDurationSeconds;

    public void loginFailed(String username) {
        if (username == null) {
            return;
        }
        Attempts current = attempts.computeIfAbsent(key(username), k -> new Attempts());
        current.lastFailure = Instant.now();
        current.count.incrementAndGet();
    }

    public void loginSucceeded(String username) {
        if (username != null) {
            attempts.remove(key(username));
        }
    }

    public boolean isBlocked(String username) {
        if (username == null) {
            return false;
        }
        Attempts current = attempts.get(key(username));
        if (current == null) {
            return false;
        }
        if (Duration.between(current.lastFailure, Instant.now()).getSeconds() >= lockDurationSeconds) {
            attempts.remove(key(username));
            return false;
        }
        return current.count.get() >= maxAttempts;
    }

    private String key(String username) {
        return username.toLowerCase();
    }

    private static final class Attempts {
        private final AtomicInteger count = new AtomicInteger();
        private volatile Instant lastFailure = Instant.now();
    }
}
