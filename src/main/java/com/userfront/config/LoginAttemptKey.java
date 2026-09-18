package com.userfront.config;

import javax.servlet.http.HttpServletRequest;

public final class LoginAttemptKey {

    private LoginAttemptKey() {
    }

    public static String of(String username, HttpServletRequest request) {
        return (username == null ? "" : username) + "|" + clientIp(request);
    }

    private static String clientIp(HttpServletRequest request) {
        if (request == null) {
            return "unknown";
        }
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.trim().isEmpty()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
