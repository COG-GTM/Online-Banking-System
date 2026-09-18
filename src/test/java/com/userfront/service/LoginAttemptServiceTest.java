package com.userfront.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class LoginAttemptServiceTest {

    @Test
    void blocksAfterTooManyFailures() {
        LoginAttemptService service = new LoginAttemptService();

        for (int i = 0; i < LoginAttemptService.MAX_ATTEMPTS; i++) {
            assertFalse(service.isBlocked("alice"));
            service.loginFailed("alice");
        }

        assertTrue(service.isBlocked("alice"));
    }

    @Test
    void successfulLoginClearsFailures() {
        LoginAttemptService service = new LoginAttemptService();

        for (int i = 0; i < LoginAttemptService.MAX_ATTEMPTS; i++) {
            service.loginFailed("bob");
        }
        service.loginSucceeded("bob");

        assertFalse(service.isBlocked("bob"));
    }

    @Test
    void usernameMatchingIsCaseInsensitive() {
        LoginAttemptService service = new LoginAttemptService();

        for (int i = 0; i < LoginAttemptService.MAX_ATTEMPTS; i++) {
            service.loginFailed("Carol");
        }

        assertTrue(service.isBlocked("carol"));
    }
}
