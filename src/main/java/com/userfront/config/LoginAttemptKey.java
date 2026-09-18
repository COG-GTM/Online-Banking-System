package com.userfront.config;

import javax.servlet.http.HttpServletRequest;

public final class LoginAttemptKey {

    private LoginAttemptKey() {
    }

    /**
     * The TCP peer address of the request. Proxy headers such as X-Forwarded-For are
     * deliberately ignored: they are attacker controlled unless a trusted proxy is
     * known to rewrite them, and trusting them lets a client mint a fresh counter per
     * guess. Deployments behind a reverse proxy should let the proxy populate the
     * remote address (e.g. server.use-forward-headers with a trusted proxy list).
     */
    public static String clientAddress(HttpServletRequest request) {
        if (request == null || request.getRemoteAddr() == null) {
            return "unknown";
        }
        return request.getRemoteAddr();
    }
}
