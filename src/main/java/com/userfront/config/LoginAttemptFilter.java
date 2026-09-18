package com.userfront.config;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.userfront.service.LoginAttemptService;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class LoginAttemptFilter extends OncePerRequestFilter {

    private static final RequestMatcher LOGIN_REQUEST = new AntPathRequestMatcher("/index", "POST");

    @Autowired
    private LoginAttemptService loginAttemptService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if (LOGIN_REQUEST.matches(request)) {
            String key = LoginAttemptKey.of(request.getParameter("username"), request);
            if (loginAttemptService.isBlocked(key)) {
                response.sendRedirect(request.getContextPath() + "/index?blocked");
                return;
            }
        }
        chain.doFilter(request, response);
    }
}
