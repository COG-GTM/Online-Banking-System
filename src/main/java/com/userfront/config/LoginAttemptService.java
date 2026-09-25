package com.userfront.config;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.stereotype.Service;

@Service
public class LoginAttemptService {

    private static final int MAX_USERNAME_ATTEMPTS = 5;
    private static final int MAX_ADDRESS_ATTEMPTS = 50;
    private static final long LOCKOUT_MILLIS = 15 * 60 * 1000L;
    private static final long PURGE_INTERVAL_MILLIS = 60 * 1000L;

    private static final String USERNAME_PREFIX = "user:";
    private static final String ADDRESS_PREFIX = "ip:";

    private final Map<String, Attempts> attemptsByKey = new ConcurrentHashMap<>();
    private volatile long lastPurge = System.currentTimeMillis();

    public void loginFailed(String username, String remoteAddress) {
        purgeExpired();
        for (String key : keys(username, remoteAddress)) {
            Attempts attempts = attemptsByKey.computeIfAbsent(key, k -> new Attempts());
            if (attempts.isExpired()) {
                attempts.reset();
            }
            attempts.increment();
        }
    }

    public void loginSucceeded(String username, String remoteAddress) {
        for (String key : keys(username, remoteAddress)) {
            attemptsByKey.remove(key);
        }
    }

    public boolean isBlocked(String username, String remoteAddress) {
        for (String key : keys(username, remoteAddress)) {
            Attempts attempts = attemptsByKey.get(key);
            if (attempts == null) {
                continue;
            }
            if (attempts.isExpired()) {
                attemptsByKey.remove(key);
                continue;
            }
            if (attempts.count() >= maxAttempts(key)) {
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
        Iterator<Map.Entry<String, Attempts>> iterator = attemptsByKey.entrySet().iterator();
        while (iterator.hasNext()) {
            if (iterator.next().getValue().isExpired()) {
                iterator.remove();
            }
        }
    }

    private static final class Attempts {

        private final AtomicInteger count = new AtomicInteger();
        private volatile long lastFailure = System.currentTimeMillis();

        void increment() {
            count.incrementAndGet();
            lastFailure = System.currentTimeMillis();
        }

        void reset() {
            count.set(0);
        }

        int count() {
            return count.get();
        }

        boolean isExpired() {
            return System.currentTimeMillis() - lastFailure > LOCKOUT_MILLIS;
        }
    }
}
