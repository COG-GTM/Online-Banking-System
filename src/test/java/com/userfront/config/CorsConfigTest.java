package com.userfront.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.Collections;

import javax.servlet.http.HttpServletResponse;

import org.junit.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

public class CorsConfigTest {

    private static final String TRUSTED = "https://admin.example.com";
    private static final String DEV = "http://localhost:4200";

    private static MockHttpServletRequest request(String method, String origin) {
        MockHttpServletRequest request = new MockHttpServletRequest(method, "/api/user/all");
        request.setServerName("bank.example.com");
        request.setScheme("https");
        request.setServerPort(443);
        if (origin != null) {
            request.addHeader("Origin", origin);
        }
        return request;
    }

    private static MockHttpServletRequest preflight(String origin) {
        MockHttpServletRequest request = request("OPTIONS", origin);
        request.addHeader("Access-Control-Request-Method", "GET");
        request.addHeader("Access-Control-Request-Headers", "authorization");
        return request;
    }

    private static MockFilterChain run(CorsConfigurationSource source, MockHttpServletRequest request,
            MockHttpServletResponse response) throws Exception {
        MockFilterChain chain = new MockFilterChain();
        new CorsFilter(source).doFilter(request, response, chain);
        return chain;
    }

    private static CorsConfigurationSource source(String origins, boolean credentials) {
        return new CorsConfig().corsConfigurationSource(origins, credentials);
    }

    @Test
    public void defaultConfigSendsNoCorsHeadersToDevOrigin() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = run(source("", false), request("GET", DEV), response);

        assertNotNull(chain.getRequest());
        assertNull(response.getHeader("Access-Control-Allow-Origin"));
        assertNull(response.getHeader("Access-Control-Allow-Credentials"));
    }

    @Test
    public void defaultConfigDoesNotAnswerPreflight() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = run(source("", false), preflight(DEV), response);

        assertTrue("preflight must be rejected or passed to the security chain",
                chain.getRequest() != null || response.getStatus() == HttpServletResponse.SC_FORBIDDEN);
        assertNull(response.getHeader("Access-Control-Allow-Origin"));
        assertNull(response.getHeader("Access-Control-Allow-Credentials"));
    }

    @Test
    public void allowedOriginIsEchoedWithoutCredentialsByDefault() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = run(source(TRUSTED, false), request("GET", TRUSTED), response);

        assertNotNull(chain.getRequest());
        assertEquals(TRUSTED, response.getHeader("Access-Control-Allow-Origin"));
        assertNull(response.getHeader("Access-Control-Allow-Credentials"));
    }

    @Test
    public void credentialsOnlyForAllowedOriginWhenEnabled() throws Exception {
        CorsConfigurationSource source = source(TRUSTED, true);

        MockHttpServletResponse trusted = new MockHttpServletResponse();
        run(source, request("GET", TRUSTED), trusted);
        assertEquals(TRUSTED, trusted.getHeader("Access-Control-Allow-Origin"));
        assertEquals("true", trusted.getHeader("Access-Control-Allow-Credentials"));

        MockHttpServletResponse untrusted = new MockHttpServletResponse();
        MockFilterChain chain = run(source, request("GET", DEV), untrusted);
        assertNull(chain.getRequest());
        assertEquals(HttpServletResponse.SC_FORBIDDEN, untrusted.getStatus());
        assertNull(untrusted.getHeader("Access-Control-Allow-Origin"));
        assertNull(untrusted.getHeader("Access-Control-Allow-Credentials"));
    }

    @Test
    public void preflightFromUnlistedOriginIsRejected() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = run(source(TRUSTED, true), preflight(DEV), response);

        assertNull(chain.getRequest());
        assertEquals(HttpServletResponse.SC_FORBIDDEN, response.getStatus());
        assertNull(response.getHeader("Access-Control-Allow-Origin"));
    }

    @Test
    public void preflightFromAllowedOriginIsAnswered() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        run(source(TRUSTED, false), preflight(TRUSTED), response);

        assertEquals(HttpServletResponse.SC_OK, response.getStatus());
        assertEquals(TRUSTED, response.getHeader("Access-Control-Allow-Origin"));
        assertEquals("3600", response.getHeader("Access-Control-Max-Age"));
    }

    @Test
    public void nonCorsOptionsRequestReachesTheChain() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = run(source(TRUSTED, true), request("OPTIONS", null), response);

        assertNotNull(chain.getRequest());
        assertNull(response.getHeader("Access-Control-Allow-Origin"));
    }

    @Test
    public void parseOriginsNormalizesAndDeduplicates() {
        assertEquals(Arrays.asList("https://admin.example.com", "http://localhost:4200"),
                CorsConfig.parseOrigins(" HTTPS://Admin.Example.com , http://localhost:4200,,https://admin.example.com"));
        assertEquals(Collections.emptyList(), CorsConfig.parseOrigins(""));
        assertEquals(Collections.emptyList(), CorsConfig.parseOrigins(null));
    }

    @Test
    public void parseOriginsRejectsUnsafeEntries() {
        for (String bad : new String[] {"*", "null", "https://*.example.com", "admin.example.com",
                "ftp://admin.example.com", "https://admin.example.com/", "https://admin.example.com/app",
                "https://user@admin.example.com", "https://admin.example.com?x=1"}) {
            try {
                CorsConfig.parseOrigins(bad);
                fail("expected rejection of " + bad);
            } catch (IllegalArgumentException expected) {
                // rejected
            }
        }
    }
}
