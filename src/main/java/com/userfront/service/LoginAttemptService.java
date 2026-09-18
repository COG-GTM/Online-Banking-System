package com.userfront.service;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class LoginAttemptService {

    private final int maxAttempts;
    private final Duration lockoutDuration;
    private final Map<String, Attempts> attemptsByKey = new ConcurrentHashMap<>();

    public LoginAttemptService(@Value("${security.login.max-attempts:5}") int maxAttempts,
                               @Value("${security.login.lockout-seconds:900}") long lockoutSeconds) {
        this.maxAttempts = maxAttempts;
        this.lockoutDuration = Duration.ofSeconds(lockoutSeconds);
    }

    public void loginFailed(String username, String clientIp) {
        Instant now = Instant.now();
        for (String key : keys(username, clientIp)) {
            Attempts attempts = attemptsByKey.compute(key,
                    (k, existing) -> existing == null || existing.isExpired(now, lockoutDuration) ? new Attempts(now) : existing);
            attempts.increment(now);
        }
        purgeExpired(now);
    }

    public void loginSucceeded(String username, String clientIp) {
        for (String key : keys(username, clientIp)) {
            attemptsByKey.remove(key);
        }
    }

    public boolean isBlocked(String username, String clientIp) {
        Instant now = Instant.now();
        for (String key : keys(username, clientIp)) {
            Attempts attempts = attemptsByKey.get(key);
            if (attempts != null && !attempts.isExpired(now, lockoutDuration) && attempts.count() >= maxAttempts) {
                return true;
            }
        }
        return false;
    }

    public Duration getLockoutDuration() {
        return lockoutDuration;
    }

    private String[] keys(String username, String clientIp) {
        String user = username == null ? "" : username.toLowerCase();
        String ip = clientIp == null ? "" : clientIp;
        return new String[] { "user:" + user, "ip:" + ip };
    }

    private void purgeExpired(Instant now) {
        attemptsByKey.values().removeIf(attempts -> attempts.isExpired(now, lockoutDuration));
    }

    private static final class Attempts {

        private final AtomicInteger count = new AtomicInteger();
        private volatile Instant lastAttempt;

        private Attempts(Instant firstAttempt) {
            this.lastAttempt = firstAttempt;
        }

        private void increment(Instant now) {
            count.incrementAndGet();
            lastAttempt = now;
        }

        private int count() {
            return count.get();
        }

        private boolean isExpired(Instant now, Duration lockoutDuration) {
            return lastAttempt.plus(lockoutDuration).isBefore(now);
        }
    }
}
