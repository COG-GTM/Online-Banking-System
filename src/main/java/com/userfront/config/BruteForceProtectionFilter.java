package com.userfront.config;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import com.userfront.service.LoginAttemptService;

/**
 * Rejects sign-in submissions for a locked account or a throttled client IP before the
 * credentials are checked. The client IP is the socket address, not X-Forwarded-For, so it
 * cannot be spoofed per request.
 */
public class BruteForceProtectionFilter extends OncePerRequestFilter {

    private static final Logger LOG = LoggerFactory.getLogger(BruteForceProtectionFilter.class);

    private final RequestMatcher loginRequestMatcher;
    private final String lockedRedirectUrl;
    private final LoginAttemptService loginAttemptService;

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
            String username = request.getParameter(UsernamePasswordAuthenticationFilter.SPRING_SECURITY_FORM_USERNAME_KEY);
            String ip = request.getRemoteAddr();
            if (loginAttemptService.isBlocked(username, ip)) {
                LOG.warn("Rejected sign-in attempt from {}: too many failed attempts", ip);
                response.sendRedirect(request.getContextPath() + lockedRedirectUrl);
                return;
            }
        }
        chain.doFilter(request, response);
    }
}
