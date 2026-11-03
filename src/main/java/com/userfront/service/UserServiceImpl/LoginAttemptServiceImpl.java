package com.userfront.service.UserServiceImpl;

import java.time.Clock;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.userfront.dao.UserDao;
import com.userfront.domain.User;
import com.userfront.service.LoginAttemptService;

/**
 * Tracks failed sign-in attempts per account and per client IP. An account is locked once it
 * accumulates {@code maxAttemptsPerUser} failures (from any IP) and an IP is throttled once it
 * accumulates {@code maxAttemptsPerIp} failures (against any account). Each failure re-arms a
 * {@code lockoutMinutes} window; counters expire once that window passes without new failures.
 * Sign-ins still being checked count toward the limits, so parallel guesses cannot overshoot them.
 * Account counters are keyed by the stored username the database resolves the submitted name to, so
 * spelling variants the database treats as equal share one counter while distinct accounts never do.
 */
@Service
public class LoginAttemptServiceImpl implements LoginAttemptService {

    private static final long PURGE_INTERVAL_MILLIS = TimeUnit.MINUTES.toMillis(1);

    private final ConcurrentMap<String, Attempts> attempts = new ConcurrentHashMap<>();
    private final Map<String, Integer> inFlight = new HashMap<>();
    private final int maxAttemptsPerUser;
    private final int maxAttemptsPerIp;
    private final long lockoutMillis;
    private final Clock clock;
    private final UserDao userDao;
    private volatile long nextPurgeAt;

    @Autowired
    public LoginAttemptServiceImpl(@Value("${security.login.max-attempts-per-user:5}") int maxAttemptsPerUser,
                                   @Value("${security.login.max-attempts-per-ip:20}") int maxAttemptsPerIp,
                                   @Value("${security.login.lockout-minutes:15}") long lockoutMinutes,
                                   UserDao userDao) {
        this(maxAttemptsPerUser, maxAttemptsPerIp, lockoutMinutes, Clock.systemUTC(), userDao);
    }

    public LoginAttemptServiceImpl(int maxAttemptsPerUser, int maxAttemptsPerIp, long lockoutMinutes, Clock clock) {
        this(maxAttemptsPerUser, maxAttemptsPerIp, lockoutMinutes, clock, null);
    }

    public LoginAttemptServiceImpl(int maxAttemptsPerUser, int maxAttemptsPerIp, long lockoutMinutes, Clock clock,
                                   UserDao userDao) {
        if (maxAttemptsPerUser < 1 || maxAttemptsPerIp < 1 || lockoutMinutes < 1) {
            throw new IllegalArgumentException("Login attempt limits and lockout window must be positive");
        }
        this.maxAttemptsPerUser = maxAttemptsPerUser;
        this.maxAttemptsPerIp = maxAttemptsPerIp;
        this.lockoutMillis = TimeUnit.MINUTES.toMillis(lockoutMinutes);
        this.clock = clock;
        this.userDao = userDao;
    }

    @Override
    public boolean tryBeginAttempt(String username, String ip) {
        return tryBegin(userKey(username), ipKey(ip));
    }

    private synchronized boolean tryBegin(String user, String client) {
        long now = clock.millis();
        purgeExpiredIfDue(now);
        if (countOf(user, now) + inFlightOf(user) >= maxAttemptsPerUser
                || countOf(client, now) + inFlightOf(client) >= maxAttemptsPerIp) {
            return false;
        }
        inFlight.merge(user, 1, Integer::sum);
        inFlight.merge(client, 1, Integer::sum);
        return true;
    }

    @Override
    public void endAttempt(String username, String ip) {
        end(userKey(username), ipKey(ip));
    }

    private synchronized void end(String user, String client) {
        inFlight.computeIfPresent(user, (k, n) -> n > 1 ? n - 1 : null);
        inFlight.computeIfPresent(client, (k, n) -> n > 1 ? n - 1 : null);
    }

    @Override
    public void loginFailed(String username, String ip) {
        failed(userKey(username), ipKey(ip));
    }

    private synchronized void failed(String user, String client) {
        long now = clock.millis();
        purgeExpiredIfDue(now);
        register(user, now);
        register(client, now);
    }

    @Override
    public void loginSucceeded(String username, String ip) {
        attempts.remove(userKey(username));
    }

    @Override
    public boolean isBlocked(String username, String ip) {
        return blocked(userKey(username), ipKey(ip));
    }

    private synchronized boolean blocked(String user, String client) {
        long now = clock.millis();
        purgeExpiredIfDue(now);
        return countOf(user, now) >= maxAttemptsPerUser || countOf(client, now) >= maxAttemptsPerIp;
    }

    private int inFlightOf(String key) {
        Integer n = inFlight.get(key);
        return n == null ? 0 : n;
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

    private String userKey(String username) {
        String stored = storedUsername(username);
        return stored != null ? "account:" + stored : "user:" + normalize(username);
    }

    private String storedUsername(String username) {
        if (userDao == null || username == null || username.isEmpty()) {
            return null;
        }
        try {
            User user = userDao.findByUsername(username);
            return user == null ? null : user.getUsername();
        } catch (RuntimeException e) {
            return null;
        }
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
