package com.userfront.config;

import javax.servlet.http.Cookie;

import org.apache.tomcat.util.http.Rfc6265CookieProcessor;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SameSiteCookieConfig {

    @Bean
    public WebServerFactoryCustomizer<TomcatServletWebServerFactory> sameSiteCookieCustomizer() {
        return factory -> factory.addContextCustomizers(
                context -> context.setCookieProcessor(new SameSiteCookieProcessor("Lax")));
    }

    // The embedded Tomcat 8.5 version cannot set SameSite itself, so append it to every Set-Cookie header
    // (JSESSIONID, remember-me, XSRF-TOKEN).
    static class SameSiteCookieProcessor extends Rfc6265CookieProcessor {

        private final String sameSite;

        SameSiteCookieProcessor(String sameSite) {
            this.sameSite = sameSite;
        }

        @Override
        public String generateHeader(Cookie cookie) {
            String header = super.generateHeader(cookie);
            if (header.toLowerCase().contains("samesite=")) {
                return header;
            }
            return header + "; SameSite=" + sameSite;
        }
    }
}
