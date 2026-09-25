package com.userfront.config;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.stereotype.Service;

@Service
public class LoginAttemptService {

    private static final int MAX_ATTEMPTS = 5;
    private static final long LOCKOUT_MILLIS = 15 * 60 * 1000L;

    private final Map<String, Attempts> attemptsByKey = new ConcurrentHashMap<>();

    public void loginFailed(String username, String remoteAddress) {
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
            if (attempts.count() >= MAX_ATTEMPTS) {
                return true;
            }
        }
        return false;
    }

    private String[] keys(String username, String remoteAddress) {
        String user = username == null ? "" : username.toLowerCase();
        String address = remoteAddress == null ? "" : remoteAddress;
        return new String[] { "user:" + user, "ip:" + address };
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
