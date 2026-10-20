package com.userfront.config;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;

import org.junit.Test;

public class SignupRateLimiterTest {

    private static final class MutableClock extends Clock {
        private Instant now = Instant.parse("2026-10-20T00:00:00Z");

        void advance(Duration duration) {
            now = now.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return ZoneId.of("UTC");
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }

    private final MutableClock clock = new MutableClock();
    private final SignupRateLimiter limiter = new SignupRateLimiter(3, Duration.ofMinutes(15), clock);

    @Test
    public void blocksAttemptsBeyondTheLimitWithinTheWindow() {
        assertTrue(limiter.tryAcquire("203.0.113.7"));
        assertTrue(limiter.tryAcquire("203.0.113.7"));
        assertTrue(limiter.tryAcquire("203.0.113.7"));
        assertFalse(limiter.tryAcquire("203.0.113.7"));
        assertFalse(limiter.tryAcquire("203.0.113.7"));
    }

    @Test
    public void tracksClientsIndependently() {
        for (int i = 0; i < 3; i++) {
            limiter.tryAcquire("203.0.113.7");
        }
        assertFalse(limiter.tryAcquire("203.0.113.7"));
        assertTrue(limiter.tryAcquire("198.51.100.4"));
    }

    @Test
    public void resetsAfterTheWindowElapses() {
        for (int i = 0; i < 4; i++) {
            limiter.tryAcquire("203.0.113.7");
        }
        assertFalse(limiter.tryAcquire("203.0.113.7"));

        clock.advance(Duration.ofMinutes(15));

        assertTrue(limiter.tryAcquire("203.0.113.7"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsNonPositiveLimit() {
        new SignupRateLimiter(0, Duration.ofMinutes(1), clock);
    }
}
