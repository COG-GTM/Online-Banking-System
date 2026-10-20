package com.userfront.config;

import java.time.Clock;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Fixed-window limiter for unauthenticated signup attempts, keyed by client address.
 */
@Component
public class SignupRateLimiter {

    static final int MAX_TRACKED_CLIENTS = 10000;

    private final int maxAttempts;
    private final long windowMillis;
    private final Clock clock;
    private final ConcurrentMap<String, Window> windows = new ConcurrentHashMap<>();

    @Autowired
    public SignupRateLimiter(@Value("${signup.rate-limit.max-attempts:5}") int maxAttempts,
                             @Value("${signup.rate-limit.window-seconds:900}") long windowSeconds) {
        this(maxAttempts, Duration.ofSeconds(windowSeconds), Clock.systemUTC());
    }

    SignupRateLimiter(int maxAttempts, Duration window, Clock clock) {
        if (maxAttempts < 1) {
            throw new IllegalArgumentException("signup.rate-limit.max-attempts must be at least 1");
        }
        if (window.isZero() || window.isNegative()) {
            throw new IllegalArgumentException("signup.rate-limit.window-seconds must be positive");
        }
        this.maxAttempts = maxAttempts;
        this.windowMillis = window.toMillis();
        this.clock = clock;
    }

    public boolean tryAcquire(String clientKey) {
        final long now = clock.millis();
        if (windows.size() >= MAX_TRACKED_CLIENTS) {
            windows.entrySet().removeIf(e -> e.getValue().isExpired(now, windowMillis));
        }
        Window window = windows.compute(clientKey == null ? "" : clientKey, (key, current) -> {
            if (current == null || current.isExpired(now, windowMillis)) {
                return new Window(now, 1);
            }
            return new Window(current.start, Math.min(current.count + 1, maxAttempts + 1));
        });
        return window.count <= maxAttempts;
    }

    private static final class Window {
        private final long start;
        private final int count;

        private Window(long start, int count) {
            this.start = start;
            this.count = count;
        }

        private boolean isExpired(long now, long windowMillis) {
            return now - start >= windowMillis;
        }
    }
}
