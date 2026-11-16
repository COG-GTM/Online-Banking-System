package com.userfront.config;

import org.springframework.boot.web.servlet.server.CookieSameSiteSupplier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SameSiteCookieConfig {

    // Applies to every cookie the embedded server writes (JSESSIONID, remember-me, XSRF-TOKEN).
    @Bean
    public CookieSameSiteSupplier laxSameSiteCookies() {
        return CookieSameSiteSupplier.ofLax();
    }
}
