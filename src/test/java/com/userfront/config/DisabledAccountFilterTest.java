package com.userfront.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import javax.servlet.http.Cookie;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import com.userfront.domain.User;

class DisabledAccountFilterTest {

    private UserDetailsService userDetailsService;
    private DisabledAccountFilter filter;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;
    private MockFilterChain chain;
    private MockHttpSession session;

    @BeforeEach
    void setUp() {
        userDetailsService = mock(UserDetailsService.class);
        filter = new DisabledAccountFilter(userDetailsService);
        session = new MockHttpSession();
        request = new MockHttpServletRequest("POST", "/transfer/betweenAccounts");
        request.setSession(session);
        request.setCookies(new Cookie("remember-me", "token"));
        response = new MockHttpServletResponse();
        chain = new MockFilterChain();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void enabledUserContinues() throws Exception {
        signIn("alice");
        when(userDetailsService.loadUserByUsername("alice")).thenReturn(user("alice", true));

        filter.doFilter(request, response, chain);

        assertSame(request, chain.getRequest());
        assertFalse(session.isInvalid());
        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void disabledUserSessionIsEndedAndRequestRejected() throws Exception {
        signIn("mallory");
        when(userDetailsService.loadUserByUsername("mallory")).thenReturn(user("mallory", false));

        filter.doFilter(request, response, chain);

        assertNull(chain.getRequest());
        assertTrue(session.isInvalid());
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        assertEquals("/index?disabled", response.getRedirectedUrl());
        Cookie rememberMe = response.getCookie("remember-me");
        assertNotNull(rememberMe);
        assertEquals(0, rememberMe.getMaxAge());
    }

    @Test
    void deletedUserSessionIsEnded() throws Exception {
        signIn("ghost");
        when(userDetailsService.loadUserByUsername("ghost")).thenThrow(new UsernameNotFoundException("ghost"));

        filter.doFilter(request, response, chain);

        assertNull(chain.getRequest());
        assertTrue(session.isInvalid());
        assertEquals("/index?disabled", response.getRedirectedUrl());
    }

    @Test
    void disabledUserApiRequestGets401() throws Exception {
        request.setRequestURI("/api/user/all");
        signIn("mallory");
        when(userDetailsService.loadUserByUsername("mallory")).thenReturn(user("mallory", false));

        filter.doFilter(request, response, chain);

        assertNull(chain.getRequest());
        assertTrue(session.isInvalid());
        assertEquals(401, response.getStatus());
        assertNull(response.getRedirectedUrl());
    }

    @Test
    void unauthenticatedRequestsAreNotLookedUp() throws Exception {
        filter.doFilter(request, response, chain);
        assertSame(request, chain.getRequest());

        SecurityContextHolder.getContext().setAuthentication(new AnonymousAuthenticationToken(
                "key", "anonymousUser", AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS")));
        MockFilterChain anonymousChain = new MockFilterChain();
        filter.doFilter(request, new MockHttpServletResponse(), anonymousChain);
        assertSame(request, anonymousChain.getRequest());

        verify(userDetailsService, never()).loadUserByUsername(org.mockito.ArgumentMatchers.anyString());
    }

    private void signIn(String username) {
        Authentication auth = new UsernamePasswordAuthenticationToken(
                user(username, true), "n/a", AuthorityUtils.createAuthorityList("ROLE_USER"));
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    private static User user(String username, boolean enabled) {
        User user = new User();
        user.setUsername(username);
        user.setEnabled(enabled);
        return user;
    }
}
