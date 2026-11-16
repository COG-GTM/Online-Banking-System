package com.userfront.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import javax.servlet.http.Cookie;

import org.apache.tomcat.util.http.CookieProcessor;
import org.apache.tomcat.util.http.Rfc6265CookieProcessor;
import org.junit.jupiter.api.Test;

public class SameSiteCookieProcessorTest {

    private final CookieProcessor processor = new SameSiteCookieProcessor(new Rfc6265CookieProcessor(), "Lax");

    @Test
    public void appendsSameSiteToSessionCookie() {
        Cookie cookie = new Cookie("JSESSIONID", "abc");
        cookie.setPath("/");
        cookie.setHttpOnly(true);

        String header = processor.generateHeader(cookie);

        assertTrue(header.startsWith("JSESSIONID=abc"), header);
        assertTrue(header.endsWith("; SameSite=Lax"), header);
        assertTrue(header.contains("HttpOnly"), header);
    }

    @Test
    public void keepsExistingSameSiteAttribute() {
        CookieProcessor alreadySet = new SameSiteCookieProcessor(new SameSiteCookieProcessor(new Rfc6265CookieProcessor(), "Strict"), "Lax");

        String header = alreadySet.generateHeader(new Cookie("remember-me", "token"));

        assertTrue(header.endsWith("; SameSite=Strict"), header);
        assertEquals(header.indexOf("SameSite"), header.lastIndexOf("SameSite"));
    }
}
