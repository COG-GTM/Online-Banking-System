package com.userfront.service.UserServiceImpl;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Tracks failed authentication attempts per username and per client address and
 * blocks further login attempts once the configured threshold is reached.
 */
@Service
public class LoginAttemptService {

    private static final String USER_PREFIX = "user:";
    private static final String ADDRESS_PREFIX = "ip:";
    private static final int MAX_TRACKED_KEYS = 10000;

    private final ConcurrentMap<String, Attempts> attempts = new ConcurrentHashMap<>();
    private final int maxAttempts;
    private final long lockoutMillis;

    public LoginAttemptService(@Value("${security.login.max-attempts:5}") int maxAttempts,
                               @Value("${security.login.lockout-minutes:15}") long lockoutMinutes) {
        this.maxAttempts = maxAttempts;
        this.lockoutMillis = TimeUnit.MINUTES.toMillis(lockoutMinutes);
    }

    public void loginFailed(String username, String remoteAddress) {
        long now = System.currentTimeMillis();
        pruneExpired(now);
        recordFailure(userKey(username), now);
        recordFailure(addressKey(remoteAddress), now);
    }

    public void loginSucceeded(String username, String remoteAddress) {
        attempts.remove(userKey(username));
        attempts.remove(addressKey(remoteAddress));
    }

    public boolean isBlocked(String username, String remoteAddress) {
        long now = System.currentTimeMillis();
        return isKeyBlocked(userKey(username), now) || isKeyBlocked(addressKey(remoteAddress), now);
    }

    /**
     * Remaining lockout for the given credentials, in seconds, or zero when not locked out.
     */
    public long blockedForSeconds(String username, String remoteAddress) {
        long now = System.currentTimeMillis();
        long remaining = Math.max(remainingMillis(userKey(username), now), remainingMillis(addressKey(remoteAddress), now));
        return TimeUnit.MILLISECONDS.toSeconds(remaining);
    }

    private void recordFailure(String key, long now) {
        if (key == null) {
            return;
        }
        attempts.merge(key, new Attempts(1, now), (existing, fresh) ->
                existing.isExpired(now, lockoutMillis) ? fresh : new Attempts(existing.count + 1, now));
    }

    private boolean isKeyBlocked(String key, long now) {
        if (key == null) {
            return false;
        }
        Attempts current = attempts.get(key);
        return current != null && !current.isExpired(now, lockoutMillis) && current.count >= maxAttempts;
    }

    private long remainingMillis(String key, long now) {
        if (key == null) {
            return 0L;
        }
        Attempts current = attempts.get(key);
        if (current == null || current.isExpired(now, lockoutMillis) || current.count < maxAttempts) {
            return 0L;
        }
        return Math.max(0L, current.lastFailure + lockoutMillis - now);
    }

    private void pruneExpired(long now) {
        if (attempts.size() < MAX_TRACKED_KEYS) {
            return;
        }
        Iterator<Map.Entry<String, Attempts>> iterator = attempts.entrySet().iterator();
        while (iterator.hasNext()) {
            if (iterator.next().getValue().isExpired(now, lockoutMillis)) {
                iterator.remove();
            }
        }
    }

    private String userKey(String username) {
        return isEmpty(username) ? null : USER_PREFIX + username.trim().toLowerCase();
    }

    private String addressKey(String remoteAddress) {
        return isEmpty(remoteAddress) ? null : ADDRESS_PREFIX + remoteAddress.trim();
    }

    private static boolean isEmpty(String value) {
        return value == null || value.trim().isEmpty();
    }

    private static final class Attempts {

        private final int count;
        private final long lastFailure;

        private Attempts(int count, long lastFailure) {
            this.count = count;
            this.lastFailure = lastFailure;
        }

        private boolean isExpired(long now, long lockoutMillis) {
            return now - lastFailure >= lockoutMillis;
        }
    }
}
