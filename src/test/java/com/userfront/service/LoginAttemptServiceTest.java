package com.userfront.service;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class LoginAttemptServiceTest {

    private static final String USERNAME = "jsmith";
    private static final String IP = "10.0.0.1";

    @Test
    public void blocksAfterMaxAttempts() {
        LoginAttemptService service = new LoginAttemptService(3, 900);

        service.loginFailed(USERNAME, IP);
        service.loginFailed(USERNAME, IP);
        assertFalse(service.isBlocked(USERNAME, IP));

        service.loginFailed(USERNAME, IP);
        assertTrue(service.isBlocked(USERNAME, IP));
    }

    @Test
    public void blocksSameUsernameFromAnotherIp() {
        LoginAttemptService service = new LoginAttemptService(2, 900);

        service.loginFailed(USERNAME, IP);
        service.loginFailed(USERNAME, "10.0.0.2");

        assertTrue(service.isBlocked(USERNAME, "10.0.0.3"));
    }

    @Test
    public void blocksSameIpAcrossUsernames() {
        LoginAttemptService service = new LoginAttemptService(2, 900);

        service.loginFailed("alice", IP);
        service.loginFailed("bob", IP);

        assertTrue(service.isBlocked("carol", IP));
    }

    @Test
    public void successResetsCounters() {
        LoginAttemptService service = new LoginAttemptService(2, 900);

        service.loginFailed(USERNAME, IP);
        service.loginSucceeded(USERNAME, IP);
        service.loginFailed(USERNAME, IP);

        assertFalse(service.isBlocked(USERNAME, IP));
    }

    @Test
    public void lockoutExpires() throws InterruptedException {
        LoginAttemptService service = new LoginAttemptService(1, 1);

        service.loginFailed(USERNAME, IP);
        assertTrue(service.isBlocked(USERNAME, IP));

        Thread.sleep(1100);
        assertFalse(service.isBlocked(USERNAME, IP));
    }

    @Test
    public void treatsUsernameCaseInsensitively() {
        LoginAttemptService service = new LoginAttemptService(2, 900);

        service.loginFailed("JSmith", IP);
        service.loginFailed("jsmith", "10.0.0.2");

        assertTrue(service.isBlocked("JSMITH", "10.0.0.3"));
    }
}
