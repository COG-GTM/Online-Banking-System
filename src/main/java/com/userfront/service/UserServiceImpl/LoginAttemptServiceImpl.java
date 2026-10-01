package com.userfront.service.UserServiceImpl;

import java.time.Clock;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.userfront.service.LoginAttemptService;

/**
 * Tracks failed sign-in attempts per account and per client IP. An account is locked once it
 * accumulates {@code maxAttemptsPerUser} failures (from any IP) and an IP is throttled once it
 * accumulates {@code maxAttemptsPerIp} failures (against any account). Each failure re-arms a
 * {@code lockoutMinutes} window; counters expire once that window passes without new failures.
 */
@Service
public class LoginAttemptServiceImpl implements LoginAttemptService {

    private static final long PURGE_INTERVAL_MILLIS = TimeUnit.MINUTES.toMillis(1);

    private final ConcurrentMap<String, Attempts> attempts = new ConcurrentHashMap<>();
    private final int maxAttemptsPerUser;
    private final int maxAttemptsPerIp;
    private final long lockoutMillis;
    private final Clock clock;
    private volatile long nextPurgeAt;

    @Autowired
    public LoginAttemptServiceImpl(@Value("${security.login.max-attempts-per-user:5}") int maxAttemptsPerUser,
                                   @Value("${security.login.max-attempts-per-ip:20}") int maxAttemptsPerIp,
                                   @Value("${security.login.lockout-minutes:15}") long lockoutMinutes) {
        this(maxAttemptsPerUser, maxAttemptsPerIp, lockoutMinutes, Clock.systemUTC());
    }

    public LoginAttemptServiceImpl(int maxAttemptsPerUser, int maxAttemptsPerIp, long lockoutMinutes, Clock clock) {
        if (maxAttemptsPerUser < 1 || maxAttemptsPerIp < 1 || lockoutMinutes < 1) {
            throw new IllegalArgumentException("Login attempt limits and lockout window must be positive");
        }
        this.maxAttemptsPerUser = maxAttemptsPerUser;
        this.maxAttemptsPerIp = maxAttemptsPerIp;
        this.lockoutMillis = TimeUnit.MINUTES.toMillis(lockoutMinutes);
        this.clock = clock;
    }

    @Override
    public void loginFailed(String username, String ip) {
        long now = clock.millis();
        purgeExpiredIfDue(now);
        register(userKey(username), now);
        register(ipKey(ip), now);
    }

    @Override
    public void loginSucceeded(String username, String ip) {
        attempts.remove(userKey(username));
    }

    @Override
    public boolean isBlocked(String username, String ip) {
        long now = clock.millis();
        purgeExpiredIfDue(now);
        return countOf(userKey(username), now) >= maxAttemptsPerUser
                || countOf(ipKey(ip), now) >= maxAttemptsPerIp;
    }

    private void register(String key, long now) {
        final long expiresAt = now + lockoutMillis;
        attempts.compute(key, (k, current) ->
                current == null || current.isExpired(now)
                        ? new Attempts(1, expiresAt)
                        : new Attempts(current.count + 1, expiresAt));
    }

    private int countOf(String key, long now) {
        Attempts current = attempts.get(key);
        return current == null || current.isExpired(now) ? 0 : current.count;
    }

    private void purgeExpiredIfDue(long now) {
        if (now < nextPurgeAt) {
            return;
        }
        nextPurgeAt = now + PURGE_INTERVAL_MILLIS;
        attempts.values().removeIf(a -> a.isExpired(now));
    }

    private static String userKey(String username) {
        return "user:" + normalize(username);
    }

    private static String ipKey(String ip) {
        return "ip:" + normalize(ip);
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private static final class Attempts {

        private final int count;
        private final long expiresAt;

        private Attempts(int count, long expiresAt) {
            this.count = count;
            this.expiresAt = expiresAt;
        }

        private boolean isExpired(long now) {
            return expiresAt <= now;
        }
    }
}
