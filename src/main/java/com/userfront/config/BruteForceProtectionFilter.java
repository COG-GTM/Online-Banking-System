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
 * Rejects login submissions coming from a username/IP pair that has exceeded the
 * allowed number of failed authentication attempts.
 */
public class BruteForceProtectionFilter extends OncePerRequestFilter {

    private static final Logger LOG = LoggerFactory.getLogger(BruteForceProtectionFilter.class);

    private final RequestMatcher loginRequestMatcher;
    private final LoginAttemptService loginAttemptService;
    private final String lockedRedirectUrl;

    public BruteForceProtectionFilter(String loginProcessingUrl, String lockedRedirectUrl,
                                      LoginAttemptService loginAttemptService) {
        this.loginRequestMatcher = new AntPathRequestMatcher(loginProcessingUrl, "POST");
        this.lockedRedirectUrl = lockedRedirectUrl;
        this.loginAttemptService = loginAttemptService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if (loginRequestMatcher.matches(request)) {
            String username = request.getParameter("username");
            String ip = request.getRemoteAddr();
            if (loginAttemptService.isBlocked(username, ip)) {
                LOG.warn("Blocked login attempt for username {} from {}: too many failed attempts", username, ip);
                response.sendRedirect(request.getContextPath() + lockedRedirectUrl);
                return;
            }
        }
        chain.doFilter(request, response);
    }
}
