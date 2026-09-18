package com.userfront.config;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import com.userfront.service.UserServiceImpl.LoginAttemptService;

/**
 * Rejects login submissions coming from a username or client address that has
 * exceeded the allowed number of failed authentication attempts.
 */
public class BruteForceProtectionFilter extends OncePerRequestFilter {

    private static final String USERNAME_PARAMETER = "username";
    private static final String LOCKED_URL = "/index?locked";

    private final LoginAttemptService loginAttemptService;
    private final RequestMatcher loginMatcher;

    public BruteForceProtectionFilter(LoginAttemptService loginAttemptService, String loginProcessingUrl) {
        this.loginAttemptService = loginAttemptService;
        this.loginMatcher = new AntPathRequestMatcher(loginProcessingUrl, "POST");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if (!loginMatcher.matches(request)) {
            chain.doFilter(request, response);
            return;
        }

        String username = request.getParameter(USERNAME_PARAMETER);
        String remoteAddress = request.getRemoteAddr();
        if (!loginAttemptService.isBlocked(username, remoteAddress)) {
            chain.doFilter(request, response);
            return;
        }

        long retryAfter = loginAttemptService.blockedForSeconds(username, remoteAddress);
        response.setHeader("Retry-After", String.valueOf(retryAfter));
        response.sendRedirect(request.getContextPath() + LOCKED_URL);
    }
}
