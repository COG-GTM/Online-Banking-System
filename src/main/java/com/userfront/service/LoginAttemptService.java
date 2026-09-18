package com.userfront.service;

public interface LoginAttemptService {

    void loginFailed(String username, String clientIp);

    void loginSucceeded(String username, String clientIp);

    boolean isBlocked(String username, String clientIp);
}
