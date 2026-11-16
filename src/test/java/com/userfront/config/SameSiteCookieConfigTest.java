package com.userfront.config;

import static org.junit.Assert.assertTrue;

import javax.servlet.http.Cookie;

import org.junit.Test;

public class SameSiteCookieConfigTest {

    @Test
    public void appendsSameSiteLaxToSessionCookie() {
        Cookie cookie = new Cookie("JSESSIONID", "abc123");
        cookie.setPath("/");
        cookie.setHttpOnly(true);

        String header = new SameSiteCookieConfig.SameSiteCookieProcessor("Lax").generateHeader(cookie);

        assertTrue(header, header.startsWith("JSESSIONID=abc123"));
        assertTrue(header, header.endsWith("; SameSite=Lax"));
    }
}
