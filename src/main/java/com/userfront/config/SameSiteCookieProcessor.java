package com.userfront.config;

import java.nio.charset.Charset;
import java.util.Locale;

import javax.servlet.http.Cookie;

import org.apache.tomcat.util.http.CookieProcessor;
import org.apache.tomcat.util.http.MimeHeaders;
import org.apache.tomcat.util.http.ServerCookies;

/**
 * Adds a SameSite attribute to every Set-Cookie header Tomcat writes (JSESSIONID, remember-me, XSRF-TOKEN).
 * Implemented as a delegating wrapper so it works on Tomcat versions without native SameSite support.
 */
public class SameSiteCookieProcessor implements CookieProcessor {

    private final CookieProcessor delegate;

    private final String sameSite;

    public SameSiteCookieProcessor(CookieProcessor delegate, String sameSite) {
        this.delegate = delegate;
        this.sameSite = sameSite;
    }

    @Override
    public void parseCookieHeader(MimeHeaders headers, ServerCookies serverCookies) {
        delegate.parseCookieHeader(headers, serverCookies);
    }

    @Override
    public String generateHeader(Cookie cookie) {
        String header = delegate.generateHeader(cookie);
        if (header.toLowerCase(Locale.ROOT).contains("; samesite=")) {
            return header;
        }
        return header + "; SameSite=" + sameSite;
    }

    @Override
    public Charset getCharset() {
        return delegate.getCharset();
    }
}
