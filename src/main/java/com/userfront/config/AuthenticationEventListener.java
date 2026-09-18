package com.userfront.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AuthenticationFailureBadCredentialsEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.WebAuthenticationDetails;
import org.springframework.stereotype.Component;

import com.userfront.service.UserServiceImpl.LoginAttemptService;

/**
 * Feeds authentication outcomes into the {@link LoginAttemptService} counters.
 */
@Component
public class AuthenticationEventListener {

    @Autowired
    private LoginAttemptService loginAttemptService;

    @EventListener
    public void onAuthenticationFailure(AuthenticationFailureBadCredentialsEvent event) {
        Authentication authentication = event.getAuthentication();
        loginAttemptService.loginFailed(username(authentication), remoteAddress(authentication));
    }

    @EventListener
    public void onAuthenticationSuccess(AuthenticationSuccessEvent event) {
        Authentication authentication = event.getAuthentication();
        loginAttemptService.loginSucceeded(username(authentication), remoteAddress(authentication));
    }

    private String username(Authentication authentication) {
        return authentication == null ? null : authentication.getName();
    }

    private String remoteAddress(Authentication authentication) {
        if (authentication == null || !(authentication.getDetails() instanceof WebAuthenticationDetails)) {
            return null;
        }
        return ((WebAuthenticationDetails) authentication.getDetails()).getRemoteAddress();
    }
}
