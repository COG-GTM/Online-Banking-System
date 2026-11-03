package com.userfront.service.UserServiceImpl;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.userfront.dao.UserDao;
import com.userfront.domain.User;
import com.userfront.service.LoginAttemptService;

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
    public void accountCounterFollowsTheStoredUsernameTheDatabaseResolves() {
        UserDao userDao = Mockito.mock(UserDao.class);
        Mockito.when(userDao.findByUsername("alice")).thenReturn(user("alice"));
        Mockito.when(userDao.findByUsername("ALICE")).thenReturn(user("alice"));
        Mockito.when(userDao.findByUsername("alice  ")).thenReturn(user("alice"));
        service = new LoginAttemptServiceImpl(3, 20, 15, clock, userDao);

        service.loginFailed("alice", "10.0.0.1");
        service.loginFailed("ALICE", "10.0.0.2");
        service.loginFailed("alice  ", "10.0.0.3");

        assertTrue(service.isBlocked("alice", "10.0.0.4"));
    }

    @Test
    public void distinctStoredAccountsNeverShareACounter() {
        UserDao userDao = Mockito.mock(UserDao.class);
        Mockito.when(userDao.findByUsername("Bob")).thenReturn(user("Bob"));
        Mockito.when(userDao.findByUsername("bob")).thenReturn(user("bob"));
        service = new LoginAttemptServiceImpl(3, 20, 15, clock, userDao);

        fail("Bob", "10.0.0.1", 3);

        assertTrue(service.isBlocked("Bob", "10.0.0.2"));
        assertFalse(service.isBlocked("bob", "10.0.0.2"));
    }

    private static User user(String username) {
        User user = new User();
        user.setUsername(username);
        return user;
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

    @Test
    public void inFlightAttemptsCountTowardTheAccountLimit() {
        fail("alice", "10.0.0.1", 1);
        assertNotNull(service.tryBeginAttempt("alice", "10.0.0.2"));
        assertNotNull(service.tryBeginAttempt("alice", "10.0.0.3"));

        assertNull(service.tryBeginAttempt("alice", "10.0.0.4"));
        assertNotNull(service.tryBeginAttempt("bob", "10.0.0.4"));
    }

    @Test
    public void inFlightAttemptsCountTowardTheIpLimit() {
        for (int i = 0; i < 5; i++) {
            assertNotNull(service.tryBeginAttempt("user" + i, "10.0.0.1"));
        }

        assertNull(service.tryBeginAttempt("user5", "10.0.0.1"));
    }

    @Test
    public void endingAnAttemptFreesItsSlot() {
        LoginAttemptService.Reservation first = service.tryBeginAttempt("alice", "10.0.0.1");
        assertNotNull(first);
        assertNotNull(service.tryBeginAttempt("alice", "10.0.0.1"));
        assertNotNull(service.tryBeginAttempt("alice", "10.0.0.1"));
        assertNull(service.tryBeginAttempt("alice", "10.0.0.1"));

        service.endAttempt(first);
        service.endAttempt(first);
        assertNotNull(service.tryBeginAttempt("alice", "10.0.0.1"));
        assertNull(service.tryBeginAttempt("alice", "10.0.0.1"));
    }

    @Test
    public void failedAttemptKeepsCountingAfterItEnds() {
        for (int i = 0; i < 3; i++) {
            LoginAttemptService.Reservation reservation = service.tryBeginAttempt("alice", "10.0.0.1");
            assertNotNull(reservation);
            service.loginFailed("alice", "10.0.0.1");
            service.endAttempt(reservation);
        }

        assertNull(service.tryBeginAttempt("alice", "10.0.0.1"));
        assertTrue(service.isBlocked("alice", "10.0.0.1"));
    }

    @Test
    public void releaseFreesTheReservedAccountSlotEvenIfTheLookupLaterFails() {
        UserDao userDao = Mockito.mock(UserDao.class);
        Mockito.when(userDao.findByUsername("alice")).thenReturn(user("alice"));
        service = new LoginAttemptServiceImpl(1, 20, 15, clock, userDao);

        LoginAttemptService.Reservation reservation = service.tryBeginAttempt("alice", "10.0.0.1");
        assertNotNull(reservation);
        Mockito.when(userDao.findByUsername("alice")).thenThrow(new IllegalStateException("db down"));
        service.endAttempt(reservation);

        Mockito.reset(userDao);
        Mockito.when(userDao.findByUsername("alice")).thenReturn(user("alice"));
        assertNotNull(service.tryBeginAttempt("alice", "10.0.0.1"));
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
