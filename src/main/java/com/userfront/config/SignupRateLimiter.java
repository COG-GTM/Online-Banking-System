package com.userfront.config;

import java.time.Clock;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Fixed-window limiter for unauthenticated signup attempts, keyed by client address.
 * Memory is bounded: once {@code maxTrackedClients} clients hold live windows, new clients
 * are refused until windows expire. State is per JVM instance.
 */
@Component
public class SignupRateLimiter {

    static final int MAX_TRACKED_CLIENTS = 10000;
    private static final long SWEEP_INTERVAL_MILLIS = 1000;

    private final int maxAttempts;
    private final int maxTrackedClients;
    private final long windowMillis;
    private final Clock clock;
    private final ConcurrentMap<String, Window> windows = new ConcurrentHashMap<>();
    private final AtomicLong nextSweepAt = new AtomicLong(Long.MIN_VALUE);
    private final AtomicLong earliestExpiry = new AtomicLong(Long.MAX_VALUE);
    private final AtomicBoolean sweeping = new AtomicBoolean();

    @Autowired
    public SignupRateLimiter(@Value("${signup.rate-limit.max-attempts:5}") int maxAttempts,
                             @Value("${signup.rate-limit.window-seconds:900}") long windowSeconds) {
        this(maxAttempts, Duration.ofSeconds(windowSeconds), Clock.systemUTC(), MAX_TRACKED_CLIENTS);
    }

    SignupRateLimiter(int maxAttempts, Duration window, Clock clock, int maxTrackedClients) {
        if (maxAttempts < 1) {
            throw new IllegalArgumentException("signup.rate-limit.max-attempts must be at least 1");
        }
        if (window.isZero() || window.isNegative()) {
            throw new IllegalArgumentException("signup.rate-limit.window-seconds must be positive");
        }
        if (maxTrackedClients < 1) {
            throw new IllegalArgumentException("maxTrackedClients must be at least 1");
        }
        this.maxAttempts = maxAttempts;
        this.maxTrackedClients = maxTrackedClients;
        this.windowMillis = window.toMillis();
        this.clock = clock;
    }

    public boolean tryAcquire(String clientKey) {
        final long now = clock.millis();
        final String key = clientKey == null ? "" : clientKey;
        if (windows.size() >= maxTrackedClients && !windows.containsKey(key)) {
            sweepExpired(now);
            if (windows.size() >= maxTrackedClients) {
                return false;
            }
        }
        Window window = windows.compute(key, (k, current) -> {
            if (current == null || current.isExpired(now, windowMillis)) {
                earliestExpiry.accumulateAndGet(now + windowMillis, Math::min);
                return new Window(now, 1);
            }
            return new Window(current.start, Math.min(current.count + 1, maxAttempts + 1));
        });
        return window.count <= maxAttempts;
    }

    /**
     * Scans the table at most once per {@link #SWEEP_INTERVAL_MILLIS}, unless the earliest
     * tracked window has already expired, so expired slots are never held past their expiry.
     */
    private void sweepExpired(long now) {
        if (now < nextSweepAt.get() && now < earliestExpiry.get()) {
            return;
        }
        if (!sweeping.compareAndSet(false, true)) {
            return;
        }
        try {
            nextSweepAt.set(now + SWEEP_INTERVAL_MILLIS);
            long earliest = Long.MAX_VALUE;
            for (Map.Entry<String, Window> e : windows.entrySet()) {
                Window w = e.getValue();
                if (w.isExpired(now, windowMillis)) {
                    windows.remove(e.getKey(), w);
                } else {
                    earliest = Math.min(earliest, w.start + windowMillis);
                }
            }
            earliestExpiry.set(earliest);
        } finally {
            sweeping.set(false);
        }
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
