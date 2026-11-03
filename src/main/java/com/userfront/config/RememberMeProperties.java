package com.userfront.config;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Remember-me settings. Remember-me is only enabled when an explicit signing key is configured,
 * so tokens never depend on a per-restart random key or a key shipped in source.
 */
@Component
public class RememberMeProperties {

    static final int MIN_KEY_LENGTH = 32;

    static final int MAX_TOKEN_VALIDITY_SECONDS = 7 * 24 * 60 * 60;

    @Value("${app.security.remember-me.key:}")
    private String key;

    @Value("${app.security.remember-me.token-validity-seconds:86400}")
    private int tokenValiditySeconds;

    @Value("${app.security.remember-me.secure-cookie:true}")
    private boolean secureCookie;

    @PostConstruct
    void validate() {
        if (isEnabled() && key.trim().length() < MIN_KEY_LENGTH) {
            throw new IllegalStateException("app.security.remember-me.key must be at least "
                    + MIN_KEY_LENGTH + " characters");
        }
        if (tokenValiditySeconds <= 0 || tokenValiditySeconds > MAX_TOKEN_VALIDITY_SECONDS) {
            throw new IllegalStateException("app.security.remember-me.token-validity-seconds must be between 1 and "
                    + MAX_TOKEN_VALIDITY_SECONDS);
        }
    }

    public boolean isEnabled() {
        return StringUtils.hasText(key);
    }

    public String getKey() {
        return key;
    }

    public int getTokenValiditySeconds() {
        return tokenValiditySeconds;
    }

    public boolean isSecureCookie() {
        return secureCookie;
    }
}
