package com.userfront.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AuthenticationFailureBadCredentialsEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.stereotype.Component;

@Component
public class AuthenticationAttemptListener {

    @Autowired
    private LoginAttemptService loginAttemptService;

    @EventListener
    public void onFailure(AuthenticationFailureBadCredentialsEvent event) {
        loginAttemptService.loginFailed(String.valueOf(event.getAuthentication().getName()));
    }

    @EventListener
    public void onSuccess(AuthenticationSuccessEvent event) {
        loginAttemptService.loginSucceeded(String.valueOf(event.getAuthentication().getName()));
    }
}
