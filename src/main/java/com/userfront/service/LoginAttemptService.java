package com.userfront.service;

public interface LoginAttemptService {

    void loginFailed(String username, String ip);

    void loginSucceeded(String username, String ip);

    boolean isBlocked(String username, String ip);
}
