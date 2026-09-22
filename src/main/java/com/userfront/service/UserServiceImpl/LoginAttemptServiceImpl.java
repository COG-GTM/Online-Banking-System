package com.userfront.service.UserServiceImpl;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.userfront.service.LoginAttemptService;

@Service
public class LoginAttemptServiceImpl implements LoginAttemptService {

    private final ConcurrentMap<String, Attempts> attempts = new ConcurrentHashMap<>();

    @Value("${security.login.max-attempts-per-user:5}")
    private int maxAttemptsPerUser;

    @Value("${security.login.max-attempts-per-ip:20}")
    private int maxAttemptsPerIp;

    @Value("${security.login.lockout-minutes:15}")
    private long lockoutMinutes;

    @Override
    public void loginFailed(String username, String ip) {
        long now = System.currentTimeMillis();
        purgeExpired(now);
        register(userKey(username, ip), now);
        register(ipKey(ip), now);
    }

    @Override
    public void loginSucceeded(String username, String ip) {
        attempts.remove(userKey(username, ip));
    }

    @Override
    public boolean isBlocked(String username, String ip) {
        long now = System.currentTimeMillis();
        purgeExpired(now);
        return countOf(userKey(username, ip), now) >= maxAttemptsPerUser
                || countOf(ipKey(ip), now) >= maxAttemptsPerIp;
    }

    private void register(String key, long now) {
        final long expiresAt = now + TimeUnit.MINUTES.toMillis(lockoutMinutes);
        attempts.compute(key, (k, current) -> {
            if (current == null || current.isExpired(now)) {
                return new Attempts(1, expiresAt);
            }
            return new Attempts(current.count + 1, expiresAt);
        });
    }

    private int countOf(String key, long now) {
        Attempts current = attempts.get(key);
        return current == null || current.isExpired(now) ? 0 : current.count;
    }

    private void purgeExpired(long now) {
        Iterator<Map.Entry<String, Attempts>> iterator = attempts.entrySet().iterator();
        while (iterator.hasNext()) {
            if (iterator.next().getValue().isExpired(now)) {
                iterator.remove();
            }
        }
    }

    private String userKey(String username, String ip) {
        return "user:" + normalize(username) + "@" + normalize(ip);
    }

    private String ipKey(String ip) {
        return "ip:" + normalize(ip);
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase();
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
