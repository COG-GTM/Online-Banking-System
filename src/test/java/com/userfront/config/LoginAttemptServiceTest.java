package com.userfront.config;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class LoginAttemptServiceTest {

    @Test
    void blocksAfterMaxFailedAttempts() {
        LoginAttemptService service = new LoginAttemptService();

        for (int i = 0; i < LoginAttemptService.MAX_ATTEMPTS - 1; i++) {
            service.loginFailed("jsmith");
        }
        assertFalse(service.isBlocked("jsmith"));

        service.loginFailed("jsmith");
        assertTrue(service.isBlocked("jsmith"));
    }

    @Test
    void successResetsAttempts() {
        LoginAttemptService service = new LoginAttemptService();

        for (int i = 0; i < LoginAttemptService.MAX_ATTEMPTS - 1; i++) {
            service.loginFailed("jsmith");
        }
        service.loginSucceeded("jsmith");
        service.loginFailed("jsmith");

        assertFalse(service.isBlocked("jsmith"));
    }
}
