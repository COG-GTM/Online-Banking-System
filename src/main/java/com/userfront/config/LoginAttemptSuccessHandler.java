package com.userfront.config;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import com.userfront.service.LoginAttemptService;

@Component
public class LoginAttemptSuccessHandler extends SavedRequestAwareAuthenticationSuccessHandler {

    @Autowired
    private LoginAttemptService loginAttemptService;

    public LoginAttemptSuccessHandler() {
        setDefaultTargetUrl("/userFront");
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
            Authentication authentication) throws IOException, ServletException {
        loginAttemptService.loginSucceeded(authentication.getName());
        super.onAuthenticationSuccess(request, response, authentication);
    }
}
