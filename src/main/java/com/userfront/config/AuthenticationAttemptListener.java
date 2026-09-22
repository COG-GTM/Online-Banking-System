package com.userfront.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AuthenticationFailureBadCredentialsEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.WebAuthenticationDetails;
import org.springframework.stereotype.Component;

import com.userfront.service.LoginAttemptService;

@Component
public class AuthenticationAttemptListener {

    private static final Logger LOG = LoggerFactory.getLogger(AuthenticationAttemptListener.class);

    @Autowired
    private LoginAttemptService loginAttemptService;

    @EventListener
    public void onFailure(AuthenticationFailureBadCredentialsEvent event) {
        String username = usernameOf(event.getAuthentication());
        String ip = ipOf(event.getAuthentication());
        loginAttemptService.loginFailed(username, ip);
        LOG.warn("Failed login attempt for username {} from {}", username, ip);
    }

    @EventListener
    public void onSuccess(AuthenticationSuccessEvent event) {
        loginAttemptService.loginSucceeded(usernameOf(event.getAuthentication()), ipOf(event.getAuthentication()));
    }

    private String usernameOf(Authentication authentication) {
        return authentication == null ? null : authentication.getName();
    }

    private String ipOf(Authentication authentication) {
        if (authentication == null || !(authentication.getDetails() instanceof WebAuthenticationDetails)) {
            return null;
        }
        return ((WebAuthenticationDetails) authentication.getDetails()).getRemoteAddress();
    }
}
