package com.userfront.security;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.Test;

public class LoginAttemptServiceTest {

    private static class MutableClock extends Clock {
        private Instant now = Instant.parse("2026-01-01T00:00:00Z");

        void advance(Duration d) {
            now = now.plus(d);
        }

        @Override
        public ZoneOffset getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(java.time.ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }

    private final MutableClock clock = new MutableClock();
    private final LoginAttemptService service = new LoginAttemptService(3, 5, Duration.ofMinutes(15), clock);

    @Test
    public void blocksUsernameAfterMaxFailures() {
        service.loginFailed("alice", "10.0.0.1");
        service.loginFailed("alice", "10.0.0.2");
        assertFalse(service.isBlocked("alice", "10.0.0.3"));
        service.loginFailed("alice", "10.0.0.3");
        assertTrue(service.isBlocked("alice", "10.0.0.4"));
    }

    @Test
    public void usernameMatchingIsCaseInsensitive() {
        service.loginFailed("Alice", null);
        service.loginFailed("ALICE ", null);
        service.loginFailed("alice", null);
        assertTrue(service.isBlocked("aLiCe", null));
    }

    @Test
    public void blocksIpAfterMaxFailuresAcrossUsernames() {
        for (int i = 0; i < 5; i++) {
            service.loginFailed("user" + i, "10.0.0.1");
        }
        assertTrue(service.isBlocked("someoneElse", "10.0.0.1"));
        assertFalse(service.isBlocked("someoneElse", "10.0.0.2"));
    }

    @Test
    public void lockoutExpires() {
        for (int i = 0; i < 3; i++) {
            service.loginFailed("alice", "10.0.0.1");
        }
        assertTrue(service.isBlocked("alice", null));
        clock.advance(Duration.ofMinutes(15));
        assertFalse(service.isBlocked("alice", null));
    }

    @Test
    public void successResetsUsernameButNotIp() {
        for (int i = 0; i < 2; i++) {
            service.loginFailed("alice", "10.0.0.1");
        }
        service.loginSucceeded("alice");
        service.loginFailed("alice", "10.0.0.1");
        service.loginFailed("alice", "10.0.0.1");
        assertFalse(service.isBlocked("alice", null));
        service.loginFailed("alice", "10.0.0.1");
        assertTrue(service.isBlocked(null, "10.0.0.1"));
    }
}
