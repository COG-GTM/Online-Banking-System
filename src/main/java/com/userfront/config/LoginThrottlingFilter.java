package com.userfront.config;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import com.userfront.service.LoginAttemptService;

/** Rejects login submissions for blocked usernames/addresses before any credential check runs. */
public class LoginThrottlingFilter extends OncePerRequestFilter {

    static final String LOCKED_URL = "/index?locked";

    private final LoginAttemptService loginAttemptService;
    private final RequestMatcher loginRequest;
    private final String usernameParameter;

    public LoginThrottlingFilter(LoginAttemptService loginAttemptService, String loginProcessingUrl, String usernameParameter) {
        this.loginAttemptService = loginAttemptService;
        this.loginRequest = new AntPathRequestMatcher(loginProcessingUrl, "POST");
        this.usernameParameter = usernameParameter;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if (loginRequest.matches(request)
                && loginAttemptService.isBlocked(request.getParameter(usernameParameter), request.getRemoteAddr())) {
            response.sendRedirect(request.getContextPath() + LOCKED_URL);
            return;
        }
        chain.doFilter(request, response);
    }
}
