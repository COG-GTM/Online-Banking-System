package com.userfront.service;

import java.time.Clock;
import java.time.Instant;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.stereotype.Service;

/**
 * Tracks failed authentication attempts and locks out the offending username and
 * client address once too many failures happen inside the attempt window.
 */
@Service
public class LoginAttemptService {

    public static final int MAX_ATTEMPTS_PER_USERNAME = 5;
    public static final int MAX_ATTEMPTS_PER_ADDRESS = 20;
    public static final long ATTEMPT_WINDOW_SECONDS = 900;
    public static final long BLOCK_DURATION_SECONDS = 900;

    private static final int MAX_TRACKED_KEYS = 20000;
    private static final String USERNAME_PREFIX = "u:";
    private static final String ADDRESS_PREFIX = "a:";

    private final Map<String, Attempts> attempts = Collections.synchronizedMap(
            new LinkedHashMap<String, Attempts>(256, 0.75f, true));

    private final Clock clock;

    public LoginAttemptService() {
        this(Clock.systemUTC());
    }

    LoginAttemptService(Clock clock) {
        this.clock = clock;
    }

    public void loginFailed(String username, String clientAddress) {
        record(usernameKey(username), MAX_ATTEMPTS_PER_USERNAME);
        record(addressKey(clientAddress), MAX_ATTEMPTS_PER_ADDRESS);
    }

    /**
     * Clears the username counter only. The address counter survives, so an attacker
     * who owns one account cannot reset the spraying limit between guesses.
     */
    public void loginSucceeded(String username) {
        attempts.remove(usernameKey(username));
    }

    public boolean isBlocked(String username, String clientAddress) {
        return isBlocked(usernameKey(username)) || isBlocked(addressKey(clientAddress));
    }

    public long secondsUntilUnblocked(String username, String clientAddress) {
        return Math.max(remainingBlockSeconds(usernameKey(username)),
                remainingBlockSeconds(addressKey(clientAddress)));
    }

    private void record(String key, int maxAttempts) {
        Instant now = clock.instant();
        synchronized (attempts) {
            Attempts current = attempts.get(key);
            if (current == null || current.isDiscardable(now)) {
                current = new Attempts(now);
                attempts.put(key, current);
            } else {
                current.increment(maxAttempts);
            }
            if (current.count >= maxAttempts && current.blockedUntil == null) {
                current.blockedUntil = now.plusSeconds(BLOCK_DURATION_SECONDS);
            }
            purgeExpired(now);
            evictUnblockedOverflow();
        }
    }

    private boolean isBlocked(String key) {
        Instant now = clock.instant();
        synchronized (attempts) {
            Attempts current = attempts.get(key);
            if (current == null) {
                return false;
            }
            if (current.isDiscardable(now)) {
                attempts.remove(key);
                return false;
            }
            return current.blockedUntil != null && now.isBefore(current.blockedUntil);
        }
    }

    private long remainingBlockSeconds(String key) {
        Instant now = clock.instant();
        synchronized (attempts) {
            Attempts current = attempts.get(key);
            if (current == null || current.blockedUntil == null || !now.isBefore(current.blockedUntil)) {
                return 0;
            }
            return current.blockedUntil.getEpochSecond() - now.getEpochSecond();
        }
    }

    private void purgeExpired(Instant now) {
        attempts.values().removeIf(entry -> entry.isDiscardable(now));
    }

    /**
     * Keeps the map bounded by dropping the least recently used entries that are not
     * under an active lockout, so flooding fresh keys cannot evict a blocked one.
     */
    private void evictUnblockedOverflow() {
        if (attempts.size() <= MAX_TRACKED_KEYS) {
            return;
        }
        Iterator<Attempts> iterator = attempts.values().iterator();
        while (attempts.size() > MAX_TRACKED_KEYS && iterator.hasNext()) {
            if (iterator.next().blockedUntil == null) {
                iterator.remove();
            }
        }
    }

    private String usernameKey(String username) {
        return USERNAME_PREFIX + (username == null ? "" : username.toLowerCase());
    }

    private String addressKey(String clientAddress) {
        return ADDRESS_PREFIX + (clientAddress == null ? "" : clientAddress);
    }

    private static final class Attempts {

        private final Instant windowStart;
        private int count;
        private Instant blockedUntil;

        private Attempts(Instant windowStart) {
            this.windowStart = windowStart;
            this.count = 1;
        }

        private void increment(int maxAttempts) {
            if (count < maxAttempts) {
                count++;
            }
        }

        /** True once the counting window has elapsed and any lockout has ended. */
        private boolean isDiscardable(Instant now) {
            if (blockedUntil != null) {
                return !now.isBefore(blockedUntil);
            }
            return !now.isBefore(windowStart.plusSeconds(ATTEMPT_WINDOW_SECONDS));
        }
    }
}
