package com.userfront.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

public class CorsAllowListTest {

    private static final String TRUSTED = "https://bank.example.com";
    private static final String UNTRUSTED = "https://evil.example.net";

    @Test
    public void parsesAndNormalizesCommaSeparatedOrigins() {
        assertEquals(Arrays.asList("https://bank.example.com", "http://localhost:4200"),
                CorsAllowList.parseOrigins(" HTTPS://Bank.Example.com , http://localhost:4200,,https://bank.example.com"));
    }

    @Test
    public void emptyOrMissingPropertyMeansNoOrigins() {
        assertTrue(CorsAllowList.parseOrigins(null).isEmpty());
        assertTrue(CorsAllowList.parseOrigins("").isEmpty());
        assertTrue(CorsAllowList.parseOrigins(" , ").isEmpty());
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsWildcard() {
        CorsAllowList.parseOrigins("https://bank.example.com,*");
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsWildcardSubdomain() {
        CorsAllowList.parseOrigins("https://*.example.com");
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsNullOrigin() {
        CorsAllowList.parseOrigins("null");
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsOriginWithPath() {
        CorsAllowList.parseOrigins("https://bank.example.com/app");
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsNonHttpScheme() {
        CorsAllowList.parseOrigins("file://bank.example.com");
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsUserInfo() {
        CorsAllowList.parseOrigins("https://user:pw@bank.example.com");
    }

    @Test(expected = IllegalArgumentException.class)
    public void sourceRequiresAtLeastOneOrigin() {
        CorsAllowList.source(Collections.<String>emptyList(), false);
    }

    @Test
    public void untrustedOriginPreflightIsRejectedWithoutCorsHeaders() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();
        filter(Collections.singletonList(TRUSTED), true).doFilter(preflight(UNTRUSTED), response, chain);

        assertEquals(403, response.getStatus());
        assertNull(response.getHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
        assertNull(response.getHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS));
        assertNull(chain.getRequest());
    }

    @Test
    public void untrustedOriginSimpleRequestIsRejected() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/userFront");
        request.addHeader(HttpHeaders.ORIGIN, UNTRUSTED);
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();
        filter(Collections.singletonList(TRUSTED), true).doFilter(request, response, chain);

        assertEquals(403, response.getStatus());
        assertNull(response.getHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
        assertNull(chain.getRequest());
    }

    @Test
    public void trustedOriginGetsEchoedOriginAndCredentialsOnlyWhenEnabled() throws Exception {
        MockHttpServletResponse withCredentials = new MockHttpServletResponse();
        filter(Collections.singletonList(TRUSTED), true).doFilter(preflight(TRUSTED), withCredentials, new MockFilterChain());
        assertEquals(TRUSTED, withCredentials.getHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
        assertEquals("true", withCredentials.getHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS));

        MockHttpServletResponse withoutCredentials = new MockHttpServletResponse();
        filter(Collections.singletonList(TRUSTED), false).doFilter(preflight(TRUSTED), withoutCredentials, new MockFilterChain());
        assertEquals(TRUSTED, withoutCredentials.getHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
        assertNull(withoutCredentials.getHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS));
    }

    private static CorsFilter filter(List<String> origins, boolean allowCredentials) {
        CorsConfigurationSource source = CorsAllowList.source(origins, allowCredentials);
        return new CorsFilter(source);
    }

    private static MockHttpServletRequest preflight(String origin) {
        MockHttpServletRequest request = new MockHttpServletRequest("OPTIONS", "/transfer/betweenAccounts");
        request.addHeader(HttpHeaders.ORIGIN, origin);
        request.addHeader(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST");
        request.addHeader(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "Content-Type");
        return request;
    }
}
