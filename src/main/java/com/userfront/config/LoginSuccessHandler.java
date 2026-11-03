package com.userfront.config;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;

import com.userfront.service.UserServiceImpl.LoginAttemptService;

public class LoginSuccessHandler extends SavedRequestAwareAuthenticationSuccessHandler {

    private final LoginAttemptService loginAttemptService;

    public LoginSuccessHandler(String defaultTargetUrl, LoginAttemptService loginAttemptService) {
        setDefaultTargetUrl(defaultTargetUrl);
        this.loginAttemptService = loginAttemptService;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws ServletException, IOException {
        loginAttemptService.loginSucceeded(authentication.getName());
        super.onAuthenticationSuccess(request, response, authentication);
    }
}
