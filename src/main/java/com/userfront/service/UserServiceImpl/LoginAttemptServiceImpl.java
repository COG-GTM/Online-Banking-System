package com.userfront.service.UserServiceImpl;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.userfront.service.LoginAttemptService;

@Service
public class LoginAttemptServiceImpl implements LoginAttemptService {

    private final Map<String, Attempts> attemptsByKey = new ConcurrentHashMap<>();

    @Value("${security.login.max-attempts:5}")
    private int maxAttempts;

    @Value("${security.login.lockout-minutes:15}")
    private long lockoutMinutes;

    @Override
    public void loginFailed(String username, String clientIp) {
        for (String key : keys(username, clientIp)) {
            Attempts attempts = attemptsByKey.computeIfAbsent(key, k -> new Attempts());
            if (attempts.isExpired(lockoutMillis())) {
                attempts.reset();
            }
            attempts.record();
        }
        purgeExpired();
    }

    @Override
    public void loginSucceeded(String username, String clientIp) {
        for (String key : keys(username, clientIp)) {
            attemptsByKey.remove(key);
        }
    }

    @Override
    public boolean isBlocked(String username, String clientIp) {
        for (String key : keys(username, clientIp)) {
            Attempts attempts = attemptsByKey.get(key);
            if (attempts != null && !attempts.isExpired(lockoutMillis()) && attempts.count() >= maxAttempts) {
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

    private void purgeExpired() {
        attemptsByKey.values().removeIf(attempts -> attempts.isExpired(lockoutMillis()));
    }

    private static final class Attempts {

        private final AtomicInteger count = new AtomicInteger();
        private volatile long lastFailure;

        void reset() {
            count.set(0);
        }

        void record() {
            count.incrementAndGet();
            lastFailure = System.currentTimeMillis();
        }

        int count() {
            return count.get();
        }

        boolean isExpired(long windowMillis) {
            return System.currentTimeMillis() - lastFailure > windowMillis;
        }
    }
}
