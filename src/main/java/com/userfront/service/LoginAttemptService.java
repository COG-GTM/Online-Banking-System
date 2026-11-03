package com.userfront.service;

public interface LoginAttemptService {

    /**
     * Reserves an in-flight sign-in attempt for the account and client IP, or returns null when
     * either is locked. Every returned reservation must be released with {@link #endAttempt}.
     */
    Reservation tryBeginAttempt(String username, String ip);

    void endAttempt(Reservation reservation);

    void loginFailed(String username, String ip);

    void loginSucceeded(String username, String ip);

    boolean isBlocked(String username, String ip);

    /** Opaque handle that releases exactly the slots {@link #tryBeginAttempt} reserved. */
    interface Reservation {
    }
}
