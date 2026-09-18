package com.userfront.service.UserServiceImpl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class LoginAttemptServiceTest {

    private static final String USERNAME = "jsmith";
    private static final String ADDRESS = "10.0.0.1";

    @Test
    public void blocksAfterMaxAttempts() {
        LoginAttemptService service = new LoginAttemptService(3, 15);

        service.loginFailed(USERNAME, ADDRESS);
        service.loginFailed(USERNAME, ADDRESS);
        assertFalse(service.isBlocked(USERNAME, ADDRESS));

        service.loginFailed(USERNAME, ADDRESS);
        assertTrue(service.isBlocked(USERNAME, ADDRESS));
    }

    @Test
    public void blocksSameUsernameFromAnotherAddress() {
        LoginAttemptService service = new LoginAttemptService(2, 15);

        service.loginFailed(USERNAME, ADDRESS);
        service.loginFailed(USERNAME, ADDRESS);

        assertTrue(service.isBlocked(USERNAME, "10.0.0.2"));
        assertFalse(service.isBlocked("other", "10.0.0.2"));
    }

    @Test
    public void blocksSameAddressAcrossUsernames() {
        LoginAttemptService service = new LoginAttemptService(2, 15);

        service.loginFailed("alice", ADDRESS);
        service.loginFailed("bob", ADDRESS);

        assertTrue(service.isBlocked("carol", ADDRESS));
    }

    @Test
    public void successResetsCounters() {
        LoginAttemptService service = new LoginAttemptService(2, 15);

        service.loginFailed(USERNAME, ADDRESS);
        service.loginSucceeded(USERNAME, ADDRESS);
        service.loginFailed(USERNAME, ADDRESS);

        assertFalse(service.isBlocked(USERNAME, ADDRESS));
    }

    @Test
    public void lockoutExpires() {
        LoginAttemptService service = new LoginAttemptService(1, 0);

        service.loginFailed(USERNAME, ADDRESS);

        assertFalse(service.isBlocked(USERNAME, ADDRESS));
        assertEquals(0L, service.blockedForSeconds(USERNAME, ADDRESS));
    }

    @Test
    public void reportsRemainingLockout() {
        LoginAttemptService service = new LoginAttemptService(1, 15);

        service.loginFailed(USERNAME, ADDRESS);

        assertTrue(service.blockedForSeconds(USERNAME, ADDRESS) > 0L);
    }

    @Test
    public void ignoresMissingCredentials() {
        LoginAttemptService service = new LoginAttemptService(1, 15);

        service.loginFailed(null, null);

        assertFalse(service.isBlocked(null, null));
    }
}
