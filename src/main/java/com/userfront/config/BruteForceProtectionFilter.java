package com.userfront.config;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.filter.OncePerRequestFilter;

import com.userfront.service.LoginAttemptService;

public class BruteForceProtectionFilter extends OncePerRequestFilter {

    private static final Logger LOG = LoggerFactory.getLogger(BruteForceProtectionFilter.class);

    static final String LOGIN_PROCESSING_URL = "/index";

    private final LoginAttemptService loginAttemptService;

    public BruteForceProtectionFilter(LoginAttemptService loginAttemptService) {
        this.loginAttemptService = loginAttemptService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if (isLoginAttempt(request)) {
            String username = request.getParameter("username");
            String clientIp = request.getRemoteAddr();
            if (loginAttemptService.isBlocked(username, clientIp)) {
                LOG.warn("Blocked login attempt for username {} from {}", username, clientIp);
                response.sendRedirect(LOGIN_PROCESSING_URL + "?locked");
                return;
            }
        }
        chain.doFilter(request, response);
    }

    private boolean isLoginAttempt(HttpServletRequest request) {
        return "POST".equalsIgnoreCase(request.getMethod())
                && LOGIN_PROCESSING_URL.equals(request.getServletPath());
    }
}
