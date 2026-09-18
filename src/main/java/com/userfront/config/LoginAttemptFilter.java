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

public class LoginAttemptFilter extends OncePerRequestFilter {

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
        if (loginRequestMatcher.matches(request)
                && loginAttemptService.isBlocked(request.getParameter("username"), ClientIpResolver.resolve(request))) {
            response.sendRedirect(request.getContextPath() + blockedUrl);
            return;
        }
        chain.doFilter(request, response);
    }
}
