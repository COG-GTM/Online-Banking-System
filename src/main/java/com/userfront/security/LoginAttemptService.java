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

    private static final class Attempts {
        private final AtomicInteger count = new AtomicInteger();
        private volatile Instant lastFailure = Instant.now();
    }

    private final Map<String, Attempts> attemptsByUsername = new ConcurrentHashMap<>();

    @Value("${app.security.login.max-attempts:5}")
    private int maxAttempts;

    @Value("${app.security.login.lockout-minutes:15}")
    private long lockoutMinutes;

    public void loginFailed(String username) {
        if (username == null) {
            return;
        }
        Attempts attempts = attemptsByUsername.computeIfAbsent(username.toLowerCase(), key -> new Attempts());
        if (isExpired(attempts)) {
            attempts.count.set(0);
        }
        attempts.count.incrementAndGet();
        attempts.lastFailure = Instant.now();
    }

    public void loginSucceeded(String username) {
        if (username != null) {
            attemptsByUsername.remove(username.toLowerCase());
        }
    }

    public boolean isBlocked(String username) {
        if (username == null) {
            return false;
        }
        Attempts attempts = attemptsByUsername.get(username.toLowerCase());
        if (attempts == null) {
            return false;
        }
        if (isExpired(attempts)) {
            attemptsByUsername.remove(username.toLowerCase());
            return false;
        }
        return attempts.count.get() >= maxAttempts;
    }

    private boolean isExpired(Attempts attempts) {
        return Duration.between(attempts.lastFailure, Instant.now()).toMinutes() >= lockoutMinutes;
    }
}
