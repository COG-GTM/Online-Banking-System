package com.userfront.config;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class LoginAttemptService {

    private static final class Attempts {
        private int count;
        private Instant lockedUntil = Instant.EPOCH;
    }

    private final Map<String, Attempts> attemptsByKey = new ConcurrentHashMap<>();

    @Value("${app.security.login.max-attempts:5}")
    private int maxAttempts;

    @Value("${app.security.login.lock-seconds:900}")
    private long lockSeconds;

    public void loginFailed(String key) {
        Attempts attempts = attemptsByKey.computeIfAbsent(key, k -> new Attempts());
        synchronized (attempts) {
            attempts.count++;
            if (attempts.count >= maxAttempts) {
                attempts.lockedUntil = Instant.now().plusSeconds(lockSeconds);
                attempts.count = 0;
            }
        }
    }

    public void loginSucceeded(String key) {
        attemptsByKey.remove(key);
    }

    public boolean isBlocked(String key) {
        Attempts attempts = attemptsByKey.get(key);
        if (attempts == null) {
            return false;
        }
        synchronized (attempts) {
            return attempts.lockedUntil.isAfter(Instant.now());
        }
    }
}
