package com.userfront.security;

import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class LoginAttemptService {

    private final Map<String, AtomicInteger> attempts = new ConcurrentHashMap<>();
    private final Map<String, Instant> blockedUntil = new ConcurrentHashMap<>();

    @Value("${app.security.login.max-attempts:5}")
    private int maxAttempts;

    @Value("${app.security.login.lockout-minutes:15}")
    private long lockoutMinutes;

    public void loginFailed(String username) {
        if (username == null) {
            return;
        }
        String key = key(username);
        int failures = attempts.computeIfAbsent(key, k -> new AtomicInteger()).incrementAndGet();
        if (failures >= maxAttempts) {
            blockedUntil.put(key, Instant.now().plus(Duration.ofMinutes(lockoutMinutes)));
            attempts.remove(key);
        }
    }

    public void loginSucceeded(String username) {
        if (username == null) {
            return;
        }
        String key = key(username);
        attempts.remove(key);
        blockedUntil.remove(key);
    }

    public boolean isBlocked(String username) {
        if (username == null) {
            return false;
        }
        Instant until = blockedUntil.get(key(username));
        if (until == null) {
            return false;
        }
        if (Instant.now().isAfter(until)) {
            blockedUntil.remove(key(username));
            return false;
        }
        return true;
    }

    private String key(String username) {
        return username.toLowerCase(Locale.ROOT);
    }
}
