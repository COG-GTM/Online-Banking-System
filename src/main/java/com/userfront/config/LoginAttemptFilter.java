package com.userfront.config;

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

import com.userfront.service.LoginAttemptService;

/**
 * Rejects login submissions coming from a client IP or targeting a username that has
 * exceeded the allowed number of failed authentication attempts.
 */
public class LoginAttemptFilter extends OncePerRequestFilter {

    private static final Logger LOG = LoggerFactory.getLogger(LoginAttemptFilter.class);

    private final LoginAttemptService loginAttemptService;
    private final RequestMatcher loginMatcher;
    private final String lockedRedirectUrl;

    public LoginAttemptFilter(LoginAttemptService loginAttemptService, String loginProcessingUrl, String lockedRedirectUrl) {
        this.loginAttemptService = loginAttemptService;
        this.loginMatcher = new AntPathRequestMatcher(loginProcessingUrl, "POST");
        this.lockedRedirectUrl = lockedRedirectUrl;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if (!loginMatcher.matches(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        String ip = request.getRemoteAddr();
        String username = request.getParameter("username");
        if (!loginAttemptService.isBlocked(ip, username)) {
            filterChain.doFilter(request, response);
            return;
        }

        long retryAfter = loginAttemptService.secondsUntilUnblocked(ip, username);
        LOG.warn("Blocked login attempt for username {} from {}; retry in {}s", username, ip, retryAfter);
        response.setHeader("Retry-After", String.valueOf(retryAfter));
        response.sendRedirect(request.getContextPath() + lockedRedirectUrl);
    }
}
