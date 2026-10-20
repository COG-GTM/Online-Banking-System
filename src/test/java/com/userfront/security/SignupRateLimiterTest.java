package com.userfront.security;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

import org.junit.Test;

public class SignupRateLimiterTest {

    private static class MutableClock extends Clock {
        private Instant now = Instant.parse("2020-01-01T00:00:00Z");

        void advance(Duration duration) {
            now = now.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
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

    @Test
    public void blocksAttemptsBeyondLimitWithinWindow() {
        SignupRateLimiter limiter = new SignupRateLimiter(3, Duration.ofMinutes(15), new MutableClock());

        assertTrue(limiter.tryAcquire("10.0.0.1"));
        assertTrue(limiter.tryAcquire("10.0.0.1"));
        assertTrue(limiter.tryAcquire("10.0.0.1"));
        assertFalse(limiter.tryAcquire("10.0.0.1"));
    }

    @Test
    public void tracksClientsIndependently() {
        SignupRateLimiter limiter = new SignupRateLimiter(1, Duration.ofMinutes(15), new MutableClock());

        assertTrue(limiter.tryAcquire("10.0.0.1"));
        assertFalse(limiter.tryAcquire("10.0.0.1"));
        assertTrue(limiter.tryAcquire("10.0.0.2"));
    }

    @Test
    public void allowsAttemptsAgainAfterWindowElapses() {
        MutableClock clock = new MutableClock();
        SignupRateLimiter limiter = new SignupRateLimiter(1, Duration.ofMinutes(15), clock);

        assertTrue(limiter.tryAcquire("10.0.0.1"));
        assertFalse(limiter.tryAcquire("10.0.0.1"));

        clock.advance(Duration.ofMinutes(15));

        assertTrue(limiter.tryAcquire("10.0.0.1"));
    }
}
