package com.userfront.config;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ClientIpResolver {

    private static final String FORWARDED_FOR = "X-Forwarded-For";

    /**
     * Only enable when every request reaches the application through a trusted
     * proxy that overwrites the header; a client can otherwise set it freely.
     */
    @Value("${security.login.trust-forwarded-for:false}")
    private boolean trustForwardedFor;

    public String resolve(HttpServletRequest request) {
        if (request == null) {
            return "";
        }
        if (trustForwardedFor) {
            String forwardedFor = request.getHeader(FORWARDED_FOR);
            if (forwardedFor != null && !forwardedFor.trim().isEmpty()) {
                return forwardedFor.split(",")[0].trim();
            }
        }
        return request.getRemoteAddr();
    }
}
