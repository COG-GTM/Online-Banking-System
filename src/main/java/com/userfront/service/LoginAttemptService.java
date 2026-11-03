package com.userfront.service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Counts failed form logins per username and per client address and blocks further attempts
 * once a threshold is reached. A block lasts {@code lockoutDuration} from the last recorded failure.
 */
@Service
public class LoginAttemptService {

    private static final int MAX_TRACKED_KEYS = 10_000;

    private final int maxUserFailures;
    private final int maxAddressFailures;
    private final Duration lockoutDuration;
    private final Clock clock;

    private final Map<String, Attempts> userAttempts = new ConcurrentHashMap<>();
    private final Map<String, Attempts> addressAttempts = new ConcurrentHashMap<>();

    @Autowired
    public LoginAttemptService(
            @Value("${security.login.max-failures-per-user:5}") int maxUserFailures,
            @Value("${security.login.max-failures-per-address:20}") int maxAddressFailures,
            @Value("${security.login.lockout-minutes:15}") long lockoutMinutes) {
        this(maxUserFailures, maxAddressFailures, Duration.ofMinutes(lockoutMinutes), Clock.systemUTC());
    }

    public LoginAttemptService(int maxUserFailures, int maxAddressFailures, Duration lockoutDuration, Clock clock) {
        if (maxUserFailures < 1 || maxAddressFailures < 1 || lockoutDuration.isNegative() || lockoutDuration.isZero()) {
            throw new IllegalArgumentException("Login throttling limits must be positive");
        }
        this.maxUserFailures = maxUserFailures;
        this.maxAddressFailures = maxAddressFailures;
        this.lockoutDuration = lockoutDuration;
        this.clock = clock;
    }

    public boolean isBlocked(String username, String address) {
        Instant now = clock.instant();
        return isBlocked(userAttempts, normalize(username), maxUserFailures, now)
                || isBlocked(addressAttempts, address, maxAddressFailures, now);
    }

    public void loginFailed(String username, String address) {
        Instant now = clock.instant();
        record(userAttempts, normalize(username), now);
        record(addressAttempts, address, now);
    }

    /** Clears the username counter only; the address counter keeps running so a valid login cannot reset it. */
    public void loginSucceeded(String username) {
        String key = normalize(username);
        if (key != null) {
            userAttempts.remove(key);
        }
    }

    private boolean isBlocked(Map<String, Attempts> attempts, String key, int max, Instant now) {
        if (key == null) {
            return false;
        }
        Attempts current = attempts.get(key);
        return current != null && !current.isExpired(now, lockoutDuration) && current.failures >= max;
    }

    private void record(Map<String, Attempts> attempts, String key, Instant now) {
        if (key == null) {
            return;
        }
        if (attempts.size() >= MAX_TRACKED_KEYS) {
            attempts.values().removeIf(a -> a.isExpired(now, lockoutDuration));
        }
        attempts.compute(key, (k, existing) -> existing == null || existing.isExpired(now, lockoutDuration)
                ? new Attempts(1, now)
                : new Attempts(existing.failures + 1, now));
    }

    private static String normalize(String username) {
        if (username == null) {
            return null;
        }
        String trimmed = username.trim();
        return trimmed.isEmpty() ? null : trimmed.toLowerCase(Locale.ROOT);
    }

    private static final class Attempts {
        private final int failures;
        private final Instant lastFailure;

        private Attempts(int failures, Instant lastFailure) {
            this.failures = failures;
            this.lastFailure = lastFailure;
        }

        private boolean isExpired(Instant now, Duration lockoutDuration) {
            return !now.isBefore(lastFailure.plus(lockoutDuration));
        }
    }
}
