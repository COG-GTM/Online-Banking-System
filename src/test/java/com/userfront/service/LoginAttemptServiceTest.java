package com.userfront.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class LoginAttemptServiceTest {

    private static final String IP = "10.0.0.1";
    private static final String USERNAME = "jsmith";

    @Test
    public void allowsAttemptsBelowThreshold() {
        LoginAttemptService service = new LoginAttemptService(3, 15);

        service.loginFailed(IP, USERNAME);
        service.loginFailed(IP, USERNAME);

        assertFalse(service.isBlocked(IP, USERNAME));
        assertEquals(0, service.secondsUntilUnblocked(IP, USERNAME));
    }

    @Test
    public void blocksOnceThresholdIsReached() {
        LoginAttemptService service = new LoginAttemptService(3, 15);

        for (int i = 0; i < 3; i++) {
            service.loginFailed(IP, USERNAME);
        }

        assertTrue(service.isBlocked(IP, USERNAME));
        assertTrue(service.secondsUntilUnblocked(IP, USERNAME) > 0);
    }

    @Test
    public void blocksUsernameFromAnotherAddress() {
        LoginAttemptService service = new LoginAttemptService(2, 15);

        service.loginFailed(IP, USERNAME);
        service.loginFailed(IP, USERNAME);

        assertTrue(service.isBlocked("10.0.0.2", USERNAME));
        assertFalse(service.isBlocked("10.0.0.2", "someone-else"));
    }

    @Test
    public void expiresBlockAfterBlockDuration() throws InterruptedException {
        LoginAttemptService service = new LoginAttemptService(1, 0);

        service.loginFailed(IP, USERNAME);
        Thread.sleep(10);

        assertFalse(service.isBlocked(IP, USERNAME));
    }

    @Test
    public void clearsCountersOnSuccessfulLogin() {
        LoginAttemptService service = new LoginAttemptService(2, 15);

        service.loginFailed(IP, USERNAME);
        service.loginSucceeded(IP, USERNAME);
        service.loginFailed(IP, USERNAME);

        assertFalse(service.isBlocked(IP, USERNAME));
    }
}
