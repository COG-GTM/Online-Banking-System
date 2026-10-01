package com.userfront.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.time.Clock;

import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.event.AuthenticationFailureBadCredentialsEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.web.authentication.WebAuthenticationDetails;

import com.userfront.service.UserServiceImpl.LoginAttemptServiceImpl;

public class BruteForceProtectionFilterTest {

    private LoginAttemptServiceImpl loginAttemptService;
    private AuthenticationAttemptListener listener;
    private BruteForceProtectionFilter filter;

    @Before
    public void setUp() {
        loginAttemptService = new LoginAttemptServiceImpl(3, 20, 15, Clock.systemUTC());
        listener = new AuthenticationAttemptListener(loginAttemptService);
        filter = new BruteForceProtectionFilter("/index", "/index?locked", loginAttemptService);
    }

    @Test
    public void passesLoginThroughUntilThresholdThenRedirectsToLocked() throws Exception {
        for (int i = 0; i < 3; i++) {
            assertNotNull(submitLogin("alice", "10.0.0.1").getRequest());
            listener.onFailure(badCredentials("alice", "10.0.0.1"));
        }

        MockFilterChain chain = new MockFilterChain();
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(loginRequest("alice", "10.0.0.2"), response, chain);

        assertNull(chain.getRequest());
        assertEquals("/index?locked", response.getRedirectedUrl());
    }

    @Test
    public void successfulLoginClearsAccountFailures() throws Exception {
        listener.onFailure(badCredentials("alice", "10.0.0.1"));
        listener.onFailure(badCredentials("alice", "10.0.0.1"));
        listener.onSuccess(new AuthenticationSuccessEvent(token("alice", "10.0.0.1")));
        listener.onFailure(badCredentials("alice", "10.0.0.1"));

        assertNotNull(submitLogin("alice", "10.0.0.1").getRequest());
    }

    @Test
    public void ignoresRequestsOtherThanLoginPost() throws Exception {
        for (int i = 0; i < 3; i++) {
            listener.onFailure(badCredentials("alice", "10.0.0.1"));
        }

        MockHttpServletRequest get = new MockHttpServletRequest("GET", "/index");
        get.setServletPath("/index");
        get.setRemoteAddr("10.0.0.1");
        get.setParameter("username", "alice");
        MockFilterChain chain = new MockFilterChain();
        filter.doFilter(get, new MockHttpServletResponse(), chain);

        assertNotNull(chain.getRequest());
    }

    @Test
    public void forwardedForHeaderDoesNotBypassIpThrottle() throws Exception {
        LoginAttemptServiceImpl ipLimited = new LoginAttemptServiceImpl(100, 2, 15, Clock.systemUTC());
        ipLimited.loginFailed("a", "10.0.0.1");
        ipLimited.loginFailed("b", "10.0.0.1");
        filter = new BruteForceProtectionFilter("/index", "/index?locked", ipLimited);

        MockHttpServletRequest request = loginRequest("c", "10.0.0.1");
        request.addHeader("X-Forwarded-For", "203.0.113.7");
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, new MockFilterChain());

        assertEquals("/index?locked", response.getRedirectedUrl());
    }

    private MockFilterChain submitLogin(String username, String ip) throws Exception {
        MockFilterChain chain = new MockFilterChain();
        filter.doFilter(loginRequest(username, ip), new MockHttpServletResponse(), chain);
        return chain;
    }

    private static MockHttpServletRequest loginRequest(String username, String ip) {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/index");
        request.setServletPath("/index");
        request.setRemoteAddr(ip);
        request.setParameter("username", username);
        request.setParameter("password", "guess");
        return request;
    }

    private static AuthenticationFailureBadCredentialsEvent badCredentials(String username, String ip) {
        return new AuthenticationFailureBadCredentialsEvent(token(username, ip), new BadCredentialsException("Bad credentials"));
    }

    private static UsernamePasswordAuthenticationToken token(String username, String ip) {
        UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken(username, "guess");
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr(ip);
        token.setDetails(new WebAuthenticationDetails(request));
        return token;
    }
}
