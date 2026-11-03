package com.userfront.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

import org.junit.Before;
import org.junit.Test;

public class LoginAttemptServiceTest {

    private MutableClock clock;
    private LoginAttemptService service;

    @Before
    public void setUp() {
        clock = new MutableClock(Instant.parse("2026-11-03T00:00:00Z"));
        service = new LoginAttemptService(3, 5, Duration.ofMinutes(15), clock);
    }

    @Test
    public void blocksUsernameAfterMaxFailures() {
        fail("alice", "10.0.0.1", 2);
        assertFalse(service.isBlocked("alice", "10.0.0.9"));

        fail("alice", "10.0.0.2", 1);
        assertTrue(service.isBlocked("alice", "10.0.0.9"));
        assertFalse(service.isBlocked("bob", "10.0.0.9"));
    }

    @Test
    public void usernameMatchingIgnoresCaseAndSurroundingWhitespace() {
        service.loginFailed("Alice", "10.0.0.1");
        service.loginFailed(" alice ", "10.0.0.2");
        service.loginFailed("ALICE", "10.0.0.3");

        assertTrue(service.isBlocked("alice", "10.0.0.9"));
    }

    @Test
    public void blocksAddressAfterMaxFailuresAcrossUsernames() {
        for (int i = 0; i < 5; i++) {
            service.loginFailed("user" + i, "10.0.0.1");
        }

        assertTrue(service.isBlocked("someone-new", "10.0.0.1"));
        assertFalse(service.isBlocked("someone-new", "10.0.0.2"));
    }

    @Test
    public void blockExpiresAfterLockoutDurationSinceLastFailure() {
        fail("alice", "10.0.0.1", 3);

        clock.advance(Duration.ofMinutes(14));
        assertTrue(service.isBlocked("alice", "10.0.0.1"));

        clock.advance(Duration.ofMinutes(1));
        assertFalse(service.isBlocked("alice", "10.0.0.1"));

        service.loginFailed("alice", "10.0.0.1");
        assertFalse("counter restarts after expiry", service.isBlocked("alice", "10.0.0.1"));
    }

    @Test
    public void successClearsUsernameCounterButNotAddressCounter() {
        fail("alice", "10.0.0.1", 2);
        service.loginSucceeded("ALICE");
        fail("alice", "10.0.0.1", 2);
        assertFalse(service.isBlocked("alice", "10.0.0.2"));

        service.loginFailed("alice", "10.0.0.1");
        assertTrue("address counter was not reset by the successful login", service.isBlocked("other", "10.0.0.1"));
    }

    @Test
    public void blankUsernamesAreNotTrackedPerUser() {
        fail("", "10.0.0.1", 3);
        fail(null, "10.0.0.2", 3);

        assertFalse(service.isBlocked("", "10.0.0.3"));
        assertFalse(service.isBlocked(null, "10.0.0.3"));
    }

    @Test
    public void trackedKeysStayBoundedWhileAllEntriesAreRecent() {
        LoginAttemptService bounded = new LoginAttemptService(2, 2, Duration.ofMinutes(15), clock, 3);
        for (int i = 0; i < 50; i++) {
            clock.advance(Duration.ofSeconds(1));
            bounded.loginFailed("user" + i, "10.0.1." + i);
        }

        assertEquals(3, bounded.trackedUsernames());
        assertEquals(3, bounded.trackedAddresses());
    }

    @Test
    public void evictionPrefersUnlockedEntriesSoLockoutsSurvive() {
        LoginAttemptService bounded = new LoginAttemptService(2, 1000, Duration.ofMinutes(15), clock, 3);
        bounded.loginFailed("alice", "10.0.0.1");
        bounded.loginFailed("alice", "10.0.0.1");
        clock.advance(Duration.ofSeconds(1));
        bounded.loginFailed("bob", "10.0.0.1");
        clock.advance(Duration.ofSeconds(1));
        bounded.loginFailed("carol", "10.0.0.1");

        for (int i = 0; i < 100; i++) {
            clock.advance(Duration.ofSeconds(1));
            bounded.loginFailed("spray" + i, "10.0.2." + i);
        }

        assertEquals(3, bounded.trackedUsernames());
        assertTrue("oldest entry is locked, so unlocked ones are evicted instead", bounded.isBlocked("alice", "10.0.0.9"));
        bounded.loginFailed("bob", "10.0.0.1");
        assertFalse("unlocked bob was evicted, so his count restarted", bounded.isBlocked("bob", "10.0.0.9"));
    }

    @Test
    public void oldestLockIsDroppedOnlyWhenEveryEntryIsLocked() {
        LoginAttemptService bounded = new LoginAttemptService(1, 1000, Duration.ofMinutes(15), clock, 2);
        bounded.loginFailed("alice", "10.0.0.1");
        clock.advance(Duration.ofSeconds(1));
        bounded.loginFailed("bob", "10.0.0.1");
        clock.advance(Duration.ofSeconds(1));
        bounded.loginFailed("carol", "10.0.0.1");

        assertEquals(2, bounded.trackedUsernames());
        assertFalse(bounded.isBlocked("alice", "10.0.0.9"));
        assertTrue(bounded.isBlocked("bob", "10.0.0.9"));
        assertTrue(bounded.isBlocked("carol", "10.0.0.9"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsNonPositiveLimits() {
        new LoginAttemptService(0, 5, Duration.ofMinutes(15), clock);
    }

    private void fail(String username, String address, int times) {
        for (int i = 0; i < times; i++) {
            service.loginFailed(username, address);
        }
    }

    private static final class MutableClock extends Clock {
        private Instant now;

        private MutableClock(Instant now) {
            this.now = now;
        }

        private void advance(Duration duration) {
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
