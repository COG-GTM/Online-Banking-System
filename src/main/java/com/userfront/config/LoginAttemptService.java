package com.userfront.config;

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

    private final Map<String, AtomicInteger> attempts = new ConcurrentHashMap<>();

    private final Map<String, Instant> lockedUntil = new ConcurrentHashMap<>();

    public void loginFailed(String username) {
        if (username == null) {
            return;
        }
        String key = key(username);
        int failures = attempts.computeIfAbsent(key, k -> new AtomicInteger()).incrementAndGet();
        if (failures >= MAX_ATTEMPTS) {
            lockedUntil.put(key, Instant.now().plus(LOCK_DURATION));
            attempts.remove(key);
        }
    }

    public void loginSucceeded(String username) {
        if (username == null) {
            return;
        }
        String key = key(username);
        attempts.remove(key);
        lockedUntil.remove(key);
    }

    public boolean isBlocked(String username) {
        if (username == null) {
            return false;
        }
        Instant until = lockedUntil.get(key(username));
        if (until == null) {
            return false;
        }
        if (Instant.now().isAfter(until)) {
            lockedUntil.remove(key(username));
            return false;
        }
        return true;
    }

    private String key(String username) {
        return username.toLowerCase();
    }
}
