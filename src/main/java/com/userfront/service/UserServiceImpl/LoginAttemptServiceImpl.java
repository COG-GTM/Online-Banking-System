package com.userfront.service.UserServiceImpl;

import java.util.Comparator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.userfront.service.LoginAttemptService;

@Service
public class LoginAttemptServiceImpl implements LoginAttemptService {

    private final Map<String, Attempts> attemptsByKey = new ConcurrentHashMap<>();

    private final Object admissionLock = new Object();

    @Value("${security.login.max-attempts:5}")
    private int maxAttempts;

    @Value("${security.login.lockout-minutes:15}")
    private long lockoutMinutes;

    @Value("${security.login.max-tracked-keys:10000}")
    private int maxTrackedKeys;

    @Override
    public void loginFailed(String username, String clientIp) {
        for (String key : keys(username, clientIp)) {
            if (attemptsByKey.computeIfPresent(key, this::increment) != null) {
                continue;
            }
            synchronized (admissionLock) {
                makeRoomFor(key);
                attemptsByKey.compute(key, this::increment);
            }
        }
    }

    private Attempts increment(String key, Attempts current) {
        long now = System.currentTimeMillis();
        if (current == null || current.isExpired(lockoutMillis(), now)) {
            return new Attempts(1, now);
        }
        return new Attempts(current.count + 1, now);
    }

    @Override
    public void loginSucceeded(String username, String clientIp) {
        for (String key : keys(username, clientIp)) {
            attemptsByKey.remove(key);
        }
    }

    @Override
    public boolean isBlocked(String username, String clientIp) {
        long now = System.currentTimeMillis();
        for (String key : keys(username, clientIp)) {
            Attempts attempts = attemptsByKey.get(key);
            if (attempts != null && !attempts.isExpired(lockoutMillis(), now) && attempts.count >= maxAttempts) {
                return true;
            }
        }
        return false;
    }

    private String[] keys(String username, String clientIp) {
        String normalizedUsername = username == null ? "" : username.toLowerCase();
        String normalizedIp = clientIp == null ? "" : clientIp;
        return new String[] { "user:" + normalizedUsername, "ip:" + normalizedIp };
    }

    private long lockoutMillis() {
        return TimeUnit.MINUTES.toMillis(lockoutMinutes);
    }

    /**
     * Keeps the tracking map bounded so that failures against an endless stream of
     * usernames cannot grow it without limit: expired entries go first, then the
     * least recently failed one. Callers hold {@code admissionLock} so that
     * eviction and insertion of a new key happen as one admission.
     */
    private void makeRoomFor(String key) {
        if (attemptsByKey.size() < maxTrackedKeys || attemptsByKey.containsKey(key)) {
            return;
        }
        long now = System.currentTimeMillis();
        attemptsByKey.entrySet()
                .removeIf(entry -> entry.getValue().isExpired(lockoutMillis(), now));
        while (attemptsByKey.size() >= maxTrackedKeys) {
            Map.Entry<String, Attempts> oldest = attemptsByKey.entrySet().stream()
                    .min(Comparator.comparingLong(entry -> entry.getValue().lastFailure))
                    .orElse(null);
            if (oldest == null) {
                return;
            }
            attemptsByKey.remove(oldest.getKey(), oldest.getValue());
        }
    }

    /** Immutable snapshot so that counter transitions stay atomic. */
    private static final class Attempts {

        private final int count;
        private final long lastFailure;

        Attempts(int count, long lastFailure) {
            this.count = count;
            this.lastFailure = lastFailure;
        }

        boolean isExpired(long windowMillis, long now) {
            return now - lastFailure > windowMillis;
        }
    }
}
