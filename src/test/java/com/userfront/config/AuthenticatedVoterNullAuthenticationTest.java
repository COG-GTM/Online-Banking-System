package com.userfront.config;

import static org.junit.Assert.assertEquals;

import java.util.Collections;

import org.junit.Test;
import org.springframework.security.access.AccessDecisionVoter;
import org.springframework.security.access.SecurityConfig;
import org.springframework.security.access.vote.AuthenticatedVoter;

/**
 * Regression test for CVE-2024-22257: AuthenticatedVoter must not grant access
 * when it is handed a null Authentication.
 */
public class AuthenticatedVoterNullAuthenticationTest {

    private final AuthenticatedVoter voter = new AuthenticatedVoter();

    @Test
    public void nullAuthenticationIsNotFullyAuthenticated() {
        assertEquals(AccessDecisionVoter.ACCESS_DENIED, vote(AuthenticatedVoter.IS_AUTHENTICATED_FULLY));
    }

    @Test
    public void nullAuthenticationIsNotRemembered() {
        assertEquals(AccessDecisionVoter.ACCESS_DENIED, vote(AuthenticatedVoter.IS_AUTHENTICATED_REMEMBERED));
    }

    private int vote(String attribute) {
        return voter.vote(null, new Object(), Collections.singletonList(new SecurityConfig(attribute)));
    }
}
