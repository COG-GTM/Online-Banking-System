package com.userfront.service;

public interface LoginAttemptService {

    /**
     * Reserves an in-flight sign-in attempt for the account and client IP, or returns false when
     * either is locked. Every successful call must be paired with {@link #endAttempt}.
     */
    boolean tryBeginAttempt(String username, String ip);

    void endAttempt(String username, String ip);

    void loginFailed(String username, String ip);

    void loginSucceeded(String username, String ip);

    boolean isBlocked(String username, String ip);
}
