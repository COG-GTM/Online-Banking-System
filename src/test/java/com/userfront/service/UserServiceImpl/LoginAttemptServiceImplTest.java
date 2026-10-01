package com.userfront.service.UserServiceImpl;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

import org.junit.Before;
import org.junit.Test;

public class LoginAttemptServiceImplTest {

    private MutableClock clock;
    private LoginAttemptServiceImpl service;

    @Before
    public void setUp() {
        clock = new MutableClock();
        service = new LoginAttemptServiceImpl(3, 5, 15, clock);
    }

    @Test
    public void locksAccountAfterMaxFailures() {
        fail("alice", "10.0.0.1", 2);
        assertFalse(service.isBlocked("alice", "10.0.0.1"));

        fail("alice", "10.0.0.1", 1);
        assertTrue(service.isBlocked("alice", "10.0.0.1"));
        assertFalse(service.isBlocked("bob", "10.0.0.2"));
    }

    @Test
    public void accountLockoutAppliesAcrossIps() {
        service.loginFailed("alice", "10.0.0.1");
        service.loginFailed("alice", "10.0.0.2");
        service.loginFailed("alice", "10.0.0.3");

        assertTrue(service.isBlocked("alice", "10.0.0.99"));
    }

    @Test
    public void usernameMatchingIsCaseAndWhitespaceInsensitive() {
        service.loginFailed("Alice", "10.0.0.1");
        service.loginFailed(" alice ", "10.0.0.2");
        service.loginFailed("ALICE", "10.0.0.3");

        assertTrue(service.isBlocked("alice", "10.0.0.4"));
    }

    @Test
    public void throttlesIpSprayingManyAccounts() {
        for (int i = 0; i < 5; i++) {
            service.loginFailed("user" + i, "10.0.0.1");
        }

        assertTrue(service.isBlocked("never-tried", "10.0.0.1"));
        assertFalse(service.isBlocked("never-tried", "10.0.0.2"));
    }

    @Test
    public void successResetsAccountButNotIpCounter() {
        fail("alice", "10.0.0.1", 2);
        service.loginSucceeded("alice", "10.0.0.1");
        fail("alice", "10.0.0.1", 2);
        assertFalse(service.isBlocked("alice", "10.0.0.1"));

        service.loginSucceeded("alice", "10.0.0.1");
        service.loginFailed("bob", "10.0.0.1");
        assertTrue(service.isBlocked("carol", "10.0.0.1"));
    }

    @Test
    public void lockoutExpiresAfterWindowWithoutFailures() {
        fail("alice", "10.0.0.1", 3);
        clock.advance(Duration.ofMinutes(14));
        assertTrue(service.isBlocked("alice", "10.0.0.1"));

        clock.advance(Duration.ofMinutes(1));
        assertFalse(service.isBlocked("alice", "10.0.0.1"));
    }

    @Test
    public void eachFailureExtendsTheWindow() {
        fail("alice", "10.0.0.1", 2);
        clock.advance(Duration.ofMinutes(10));
        service.loginFailed("alice", "10.0.0.1");
        clock.advance(Duration.ofMinutes(10));

        assertTrue(service.isBlocked("alice", "10.0.0.1"));
    }

    @Test
    public void handlesNullUsernameAndIp() {
        fail(null, null, 3);

        assertTrue(service.isBlocked(null, null));
        assertTrue(service.isBlocked("", ""));
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsNonPositiveLimits() {
        new LoginAttemptServiceImpl(0, 5, 15, clock);
    }

    private void fail(String username, String ip, int times) {
        for (int i = 0; i < times; i++) {
            service.loginFailed(username, ip);
        }
    }

    private static final class MutableClock extends Clock {

        private Instant now = Instant.parse("2026-01-01T00:00:00Z");

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
}
