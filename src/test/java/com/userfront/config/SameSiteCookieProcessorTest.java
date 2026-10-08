package com.userfront.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import javax.servlet.http.Cookie;

import org.apache.tomcat.util.http.CookieProcessor;
import org.apache.tomcat.util.http.Rfc6265CookieProcessor;
import org.junit.Test;

public class SameSiteCookieProcessorTest {

    private final CookieProcessor processor = new SameSiteCookieProcessor(new Rfc6265CookieProcessor(), "Lax");

    @Test
    public void appendsSameSiteToSessionCookie() {
        Cookie cookie = new Cookie("JSESSIONID", "abc");
        cookie.setPath("/");
        cookie.setHttpOnly(true);

        String header = processor.generateHeader(cookie);

        assertTrue(header, header.startsWith("JSESSIONID=abc"));
        assertTrue(header, header.endsWith("; SameSite=Lax"));
        assertTrue(header, header.contains("HttpOnly"));
    }

    @Test
    public void keepsExistingSameSiteAttribute() {
        CookieProcessor alreadySet = new SameSiteCookieProcessor(new SameSiteCookieProcessor(new Rfc6265CookieProcessor(), "Strict"), "Lax");

        String header = alreadySet.generateHeader(new Cookie("remember-me", "token"));

        assertTrue(header, header.endsWith("; SameSite=Strict"));
        assertEquals(header.indexOf("SameSite"), header.lastIndexOf("SameSite"));
    }
}
