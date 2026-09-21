package com.userfront.config;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class LoginAttemptService {

    private final Map<String, AtomicInteger> attempts = new ConcurrentHashMap<>();
    private final Map<String, Long> lockedUntil = new ConcurrentHashMap<>();

    @Value("${app.security.login.max-attempts:5}")
    private int maxAttempts;

    @Value("${app.security.login.lock-minutes:15}")
    private long lockMinutes;

    public void loginFailed(String username) {
        if (username == null) {
            return;
        }
        String key = key(username);
        int failures = attempts.computeIfAbsent(key, k -> new AtomicInteger()).incrementAndGet();
        if (failures >= maxAttempts) {
            lockedUntil.put(key, System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(lockMinutes));
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
        Long until = lockedUntil.get(key(username));
        if (until == null) {
            return false;
        }
        if (until < System.currentTimeMillis()) {
            lockedUntil.remove(key(username));
            return false;
        }
        return true;
    }

    private String key(String username) {
        return username.toLowerCase();
    }
}
