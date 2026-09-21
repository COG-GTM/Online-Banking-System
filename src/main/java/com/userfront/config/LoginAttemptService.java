package com.userfront.config;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Tracks consecutive failed authentication attempts per username and blocks
 * further attempts for a cool-down period once the threshold is reached.
 */
@Service
public class LoginAttemptService {

    private final Map<String, AtomicInteger> attempts = new ConcurrentHashMap<>();
    private final Map<String, Long> blockedUntil = new ConcurrentHashMap<>();

    @Value("${app.security.login.max-attempts:5}")
    private int maxAttempts;

    @Value("${app.security.login.block-duration-ms:900000}")
    private long blockDurationMs;

    public void loginFailed(String username) {
        String key = key(username);
        int failures = attempts.computeIfAbsent(key, k -> new AtomicInteger()).incrementAndGet();
        if (failures >= maxAttempts) {
            blockedUntil.put(key, System.currentTimeMillis() + blockDurationMs);
        }
    }

    public void loginSucceeded(String username) {
        String key = key(username);
        attempts.remove(key);
        blockedUntil.remove(key);
    }

    public boolean isBlocked(String username) {
        String key = key(username);
        Long until = blockedUntil.get(key);
        if (until == null) {
            return false;
        }
        if (System.currentTimeMillis() >= until) {
            attempts.remove(key);
            blockedUntil.remove(key);
            return false;
        }
        return true;
    }

    private String key(String username) {
        return username == null ? "" : username.toLowerCase();
    }
}
