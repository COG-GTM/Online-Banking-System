package com.userfront.service;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.stereotype.Service;

@Service
public class LoginAttemptService {

    static final int MAX_ATTEMPTS = 5;
    static final Duration LOCK_DURATION = Duration.ofMinutes(15);

    private record Attempts(AtomicInteger count, Instant lastAttempt) {
    }

    private final Map<String, Attempts> attemptsByUsername = new ConcurrentHashMap<>();

    public void loginSucceeded(String username) {
        attemptsByUsername.remove(key(username));
    }

    public void loginFailed(String username) {
        attemptsByUsername.compute(key(username), (k, current) -> {
            if (current == null || isExpired(current)) {
                return new Attempts(new AtomicInteger(1), Instant.now());
            }
            current.count().incrementAndGet();
            return new Attempts(current.count(), Instant.now());
        });
    }

    public boolean isBlocked(String username) {
        Attempts attempts = attemptsByUsername.get(key(username));
        if (attempts == null) {
            return false;
        }
        if (isExpired(attempts)) {
            attemptsByUsername.remove(key(username));
            return false;
        }

        return attempts.count().get() >= MAX_ATTEMPTS;
    }

    private boolean isExpired(Attempts attempts) {
        return attempts.lastAttempt().plus(LOCK_DURATION).isBefore(Instant.now());
    }

    private String key(String username) {
        return username == null ? "" : username.toLowerCase();
    }
}
