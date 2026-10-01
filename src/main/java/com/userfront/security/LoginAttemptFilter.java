package com.userfront.security;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Rejects login attempts for usernames or client IPs that are temporarily
 * blocked by {@link LoginAttemptService}, before any password check runs.
 */
public class LoginAttemptFilter extends OncePerRequestFilter {

    private static final Logger LOG = LoggerFactory.getLogger(LoginAttemptFilter.class);

    private final LoginAttemptService loginAttemptService;
    private final RequestMatcher loginRequestMatcher;
    private final String blockedUrl;

    public LoginAttemptFilter(LoginAttemptService loginAttemptService, String loginProcessingUrl, String blockedUrl) {
        this.loginAttemptService = loginAttemptService;
        this.loginRequestMatcher = new AntPathRequestMatcher(loginProcessingUrl, "POST");
        this.blockedUrl = blockedUrl;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if (loginRequestMatcher.matches(request)) {
            String username = request.getParameter("username");
            String ip = request.getRemoteAddr();
            if (loginAttemptService.isBlocked(username, ip)) {
                LOG.warn("Blocked login attempt for username {} from {}",
                        username == null ? null : username.replaceAll("[\\r\\n]", "_"), ip);
                response.sendRedirect(request.getContextPath() + blockedUrl);
                return;
            }
        }
        chain.doFilter(request, response);
    }
}
