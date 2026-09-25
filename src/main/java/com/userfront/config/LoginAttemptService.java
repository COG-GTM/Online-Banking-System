package com.userfront.config;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;

@Service
public class LoginAttemptService {

    private static final int MAX_USERNAME_ATTEMPTS = 5;
    private static final int MAX_ADDRESS_ATTEMPTS = 20;
    private static final long LOCKOUT_MILLIS = 15 * 60 * 1000L;
    private static final long PURGE_INTERVAL_MILLIS = 60 * 1000L;

    private static final String USERNAME_PREFIX = "user:";
    private static final String ADDRESS_PREFIX = "ip:";

    private final Map<String, Attempts> attemptsByKey = new ConcurrentHashMap<>();
    private volatile long lastPurge = System.currentTimeMillis();

    public void loginFailed(String username, String remoteAddress) {
        purgeExpired();
        for (String key : keys(username, remoteAddress)) {
            attemptsByKey.compute(key, (k, attempts) ->
                    attempts == null || attempts.isExpired() ? Attempts.first() : attempts.next());
        }
    }

    public void loginSucceeded(String username, String remoteAddress) {
        for (String key : keys(username, remoteAddress)) {
            attemptsByKey.remove(key);
        }
    }

    public boolean isBlocked(String username, String remoteAddress) {
        for (String key : keys(username, remoteAddress)) {
            Attempts attempts = attemptsByKey.computeIfPresent(key, (k, current) -> current.isExpired() ? null : current);
            if (attempts != null && attempts.count() >= maxAttempts(key)) {
                return true;
            }
        }
        return false;
    }

    private int maxAttempts(String key) {
        return key.startsWith(ADDRESS_PREFIX) ? MAX_ADDRESS_ATTEMPTS : MAX_USERNAME_ATTEMPTS;
    }

    private String[] keys(String username, String remoteAddress) {
        String user = username == null ? "" : username.toLowerCase();
        String address = remoteAddress == null ? "" : remoteAddress;
        return new String[] { USERNAME_PREFIX + user, ADDRESS_PREFIX + address };
    }

    private void purgeExpired() {
        long now = System.currentTimeMillis();
        if (now - lastPurge < PURGE_INTERVAL_MILLIS) {
            return;
        }
        lastPurge = now;
        for (String key : attemptsByKey.keySet()) {
            attemptsByKey.computeIfPresent(key, (k, attempts) -> attempts.isExpired() ? null : attempts);
        }
    }

    private static final class Attempts {

        private final int count;
        private final long lastFailure;

        private Attempts(int count, long lastFailure) {
            this.count = count;
            this.lastFailure = lastFailure;
        }

        static Attempts first() {
            return new Attempts(1, System.currentTimeMillis());
        }

        Attempts next() {
            return new Attempts(count + 1, System.currentTimeMillis());
        }

        int count() {
            return count;
        }

        boolean isExpired() {
            return System.currentTimeMillis() - lastFailure > LOCKOUT_MILLIS;
        }
    }
}
