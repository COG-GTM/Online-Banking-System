package com.userfront.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.Before;
import org.junit.Test;

public class LoginAttemptServiceTest {

    private MutableClock clock;
    private LoginAttemptService service;

    @Before
    public void setUp() {
        clock = new MutableClock(Instant.parse("2020-01-01T00:00:00Z"));
        service = new LoginAttemptService(clock);
    }

    @Test
    public void blocksAfterMaxFailuresForUsername() {
        for (int i = 0; i < LoginAttemptService.MAX_ATTEMPTS_PER_USERNAME - 1; i++) {
            service.loginFailed("alice", "10.0.0.1");
            assertFalse(service.isBlocked("alice", "10.0.0.1"));
        }
        service.loginFailed("alice", "10.0.0.1");
        assertTrue(service.isBlocked("alice", "10.0.0.1"));
        assertTrue(service.isBlocked("ALICE", "10.0.0.1"));
    }

    @Test
    public void blockedUsernameIsBlockedFromAnyAddress() {
        failTimes("alice", "10.0.0.1", LoginAttemptService.MAX_ATTEMPTS_PER_USERNAME);
        assertTrue(service.isBlocked("alice", "10.0.0.9"));
    }

    @Test
    public void otherUsernamesAreNotBlocked() {
        failTimes("alice", "10.0.0.1", LoginAttemptService.MAX_ATTEMPTS_PER_USERNAME);
        assertFalse(service.isBlocked("bob", "10.0.0.1"));
    }

    @Test
    public void blocksAddressSprayingManyUsernames() {
        for (int i = 0; i < LoginAttemptService.MAX_ATTEMPTS_PER_ADDRESS; i++) {
            service.loginFailed("user" + i, "10.0.0.1");
        }
        assertTrue(service.isBlocked("fresh-user", "10.0.0.1"));
        assertFalse(service.isBlocked("fresh-user", "10.0.0.2"));
    }

    @Test
    public void lockoutExpiresAfterBlockDuration() {
        failTimes("alice", "10.0.0.1", LoginAttemptService.MAX_ATTEMPTS_PER_USERNAME);
        assertEquals(LoginAttemptService.BLOCK_DURATION_SECONDS, service.secondsUntilUnblocked("alice", "10.0.0.1"));

        clock.advanceSeconds(LoginAttemptService.BLOCK_DURATION_SECONDS);
        assertFalse(service.isBlocked("alice", "10.0.0.1"));
        assertEquals(0, service.secondsUntilUnblocked("alice", "10.0.0.1"));
    }

    @Test
    public void failuresSpreadBeyondTheWindowDoNotAccumulate() {
        for (int i = 0; i < LoginAttemptService.MAX_ATTEMPTS_PER_USERNAME * 3; i++) {
            service.loginFailed("alice", "10.0.0.1");
            clock.advanceSeconds(LoginAttemptService.ATTEMPT_WINDOW_SECONDS);
            assertFalse(service.isBlocked("alice", "10.0.0.1"));
        }
    }

    @Test
    public void successResetsTheCounter() {
        failTimes("alice", "10.0.0.1", LoginAttemptService.MAX_ATTEMPTS_PER_USERNAME - 1);
        service.loginSucceeded("alice", "10.0.0.1");

        failTimes("alice", "10.0.0.1", LoginAttemptService.MAX_ATTEMPTS_PER_USERNAME - 1);
        assertFalse(service.isBlocked("alice", "10.0.0.1"));
    }

    @Test
    public void unblockedKeyReportsNoCountdown() {
        service.loginFailed("alice", "10.0.0.1");
        assertEquals(0, service.secondsUntilUnblocked("alice", "10.0.0.1"));
    }

    private void failTimes(String username, String address, int times) {
        for (int i = 0; i < times; i++) {
            service.loginFailed(username, address);
        }
    }

    private static final class MutableClock extends Clock {

        private Instant now;

        private MutableClock(Instant now) {
            this.now = now;
        }

        private void advanceSeconds(long seconds) {
            now = now.plusSeconds(seconds);
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
}
