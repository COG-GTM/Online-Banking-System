package com.userfront.security;

import java.time.Clock;
import java.time.Duration;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Tracks failed login attempts per username and per client IP and temporarily
 * blocks further attempts once a threshold is reached (CWE-307).
 */
@Service
public class LoginAttemptService {

    private static final int MAX_TRACKED_KEYS = 100_000;

    private final int maxUsernameFailures;
    private final int maxIpFailures;
    private final long lockoutMillis;
    private final Clock clock;

    private final Map<String, Attempts> usernameAttempts = new ConcurrentHashMap<>();
    private final Map<String, Attempts> ipAttempts = new ConcurrentHashMap<>();

    @Autowired
    public LoginAttemptService(
            @Value("${security.login.max-username-failures:5}") int maxUsernameFailures,
            @Value("${security.login.max-ip-failures:20}") int maxIpFailures,
            @Value("${security.login.lockout-minutes:15}") long lockoutMinutes) {
        this(maxUsernameFailures, maxIpFailures, Duration.ofMinutes(lockoutMinutes), Clock.systemUTC());
    }

    LoginAttemptService(int maxUsernameFailures, int maxIpFailures, Duration lockout, Clock clock) {
        this.maxUsernameFailures = maxUsernameFailures;
        this.maxIpFailures = maxIpFailures;
        this.lockoutMillis = lockout.toMillis();
        this.clock = clock;
    }

    public void loginFailed(String username, String ip) {
        if (username != null) {
            recordFailure(usernameAttempts, normalize(username));
        }
        if (ip != null) {
            recordFailure(ipAttempts, ip);
        }
    }

    public void loginSucceeded(String username) {
        if (username != null) {
            usernameAttempts.remove(normalize(username));
        }
    }

    public boolean isBlocked(String username, String ip) {
        return (username != null && isBlocked(usernameAttempts, normalize(username), maxUsernameFailures))
                || (ip != null && isBlocked(ipAttempts, ip, maxIpFailures));
    }

    private void recordFailure(Map<String, Attempts> attempts, String key) {
        long now = clock.millis();
        if (attempts.size() >= MAX_TRACKED_KEYS) {
            attempts.values().removeIf(a -> a.isExpired(now, lockoutMillis));
        }
        attempts.compute(key, (k, existing) -> {
            if (existing == null || existing.isExpired(now, lockoutMillis)) {
                return new Attempts(1, now);
            }
            return new Attempts(existing.failures + 1, now);
        });
    }

    private boolean isBlocked(Map<String, Attempts> attempts, String key, int maxFailures) {
        Attempts entry = attempts.get(key);
        if (entry == null) {
            return false;
        }
        if (entry.isExpired(clock.millis(), lockoutMillis)) {
            attempts.remove(key, entry);
            return false;
        }
        return entry.failures >= maxFailures;
    }

    private static String normalize(String username) {
        return username.trim().toLowerCase(Locale.ROOT);
    }

    private static final class Attempts {
        private final int failures;
        private final long lastFailureMillis;

        private Attempts(int failures, long lastFailureMillis) {
            this.failures = failures;
            this.lastFailureMillis = lastFailureMillis;
        }

        private boolean isExpired(long now, long lockoutMillis) {
            return now - lastFailureMillis >= lockoutMillis;
        }
    }
}
