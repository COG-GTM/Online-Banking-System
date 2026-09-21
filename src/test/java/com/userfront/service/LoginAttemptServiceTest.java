package com.userfront.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class LoginAttemptServiceTest {

    @Test
    void blocksAfterMaxFailedAttempts() {
        LoginAttemptService service = new LoginAttemptService();

        for (int i = 0; i < LoginAttemptService.MAX_ATTEMPTS - 1; i++) {
            service.loginFailed("jsmith");
        }
        assertThat(service.isBlocked("jsmith")).isFalse();

        service.loginFailed("jsmith");
        assertThat(service.isBlocked("jsmith")).isTrue();
        assertThat(service.isBlocked("other")).isFalse();
    }

    @Test
    void successfulLoginClearsAttempts() {
        LoginAttemptService service = new LoginAttemptService();

        for (int i = 0; i < LoginAttemptService.MAX_ATTEMPTS; i++) {
            service.loginFailed("jsmith");
        }
        service.loginSucceeded("jsmith");

        assertThat(service.isBlocked("jsmith")).isFalse();
    }
}
