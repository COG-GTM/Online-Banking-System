package com.userfront.security;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class LoginAttemptService {

    private final Map<String, Attempts> attemptsByUsername = new ConcurrentHashMap<>();

    @Value("${app.security.login.max-attempts:5}")
    private int maxAttempts;

    @Value("${app.security.login.lock-duration-seconds:900}")
    private long lockDurationSeconds;

    public void loginFailed(String username) {
        if (username == null) {
            return;
        }
        Attempts attempts = attemptsByUsername.computeIfAbsent(key(username), k -> new Attempts());
        attempts.count.incrementAndGet();
        attempts.lastFailure = Instant.now();
    }

    public void loginSucceeded(String username) {
        if (username != null) {
            attemptsByUsername.remove(key(username));
        }
    }

    public boolean isBlocked(String username) {
        if (username == null) {
            return false;
        }
        Attempts attempts = attemptsByUsername.get(key(username));
        if (attempts == null) {
            return false;
        }
        if (Duration.between(attempts.lastFailure, Instant.now()).getSeconds() >= lockDurationSeconds) {
            attemptsByUsername.remove(key(username));
            return false;
        }
        return attempts.count.get() >= maxAttempts;
    }

    private String key(String username) {
        return username.toLowerCase();
    }

    private static final class Attempts {
        private final AtomicInteger count = new AtomicInteger();
        private volatile Instant lastFailure = Instant.now();
    }
}
