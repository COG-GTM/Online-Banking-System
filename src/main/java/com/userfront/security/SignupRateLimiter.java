package com.userfront.security;

import java.time.Clock;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Sliding-window throttle for signup submissions, keyed by client address.
 */
@Component
public class SignupRateLimiter {

    private static final int CLEANUP_THRESHOLD = 10_000;

    private final int maxAttempts;
    private final long windowMillis;
    private final Clock clock;
    private final Map<String, Deque<Long>> attempts = new ConcurrentHashMap<>();

    @Autowired
    public SignupRateLimiter(@Value("${signup.rate-limit.max-attempts:5}") int maxAttempts,
                             @Value("${signup.rate-limit.window-minutes:15}") long windowMinutes) {
        this(maxAttempts, Duration.ofMinutes(windowMinutes), Clock.systemUTC());
    }

    public SignupRateLimiter(int maxAttempts, Duration window, Clock clock) {
        this.maxAttempts = maxAttempts;
        this.windowMillis = window.toMillis();
        this.clock = clock;
    }

    /**
     * Records an attempt for the given client and returns whether it is within the allowed rate.
     */
    public boolean tryAcquire(String clientKey) {
        long now = clock.millis();
        if (attempts.size() > CLEANUP_THRESHOLD) {
            evictExpired(now);
        }

        Deque<Long> timestamps = attempts.computeIfAbsent(clientKey, k -> new ArrayDeque<>());
        synchronized (timestamps) {
            pruneExpired(timestamps, now);
            if (timestamps.size() >= maxAttempts) {
                return false;
            }
            timestamps.addLast(now);
            return true;
        }
    }

    private void pruneExpired(Deque<Long> timestamps, long now) {
        while (!timestamps.isEmpty() && now - timestamps.peekFirst() >= windowMillis) {
            timestamps.pollFirst();
        }
    }

    private void evictExpired(long now) {
        attempts.forEach((key, timestamps) -> {
            synchronized (timestamps) {
                pruneExpired(timestamps, now);
                if (timestamps.isEmpty()) {
                    attempts.remove(key, timestamps);
                }
            }
        });
    }
}
