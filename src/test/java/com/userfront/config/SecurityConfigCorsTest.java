package com.userfront.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import javax.servlet.http.HttpServletResponse;

import org.junit.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

public class SecurityConfigCorsTest {

    private static CorsConfigurationSource source(String... origins) {
        SecurityConfig config = new SecurityConfig();
        ReflectionTestUtils.setField(config, "corsAllowedOrigins", origins);
        return config.corsConfigurationSource();
    }

    private static MockHttpServletResponse preflight(CorsConfigurationSource source, String origin) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("OPTIONS", "/api/user/all");
        request.addHeader("Origin", origin);
        request.addHeader("Access-Control-Request-Method", "GET");
        MockHttpServletResponse response = new MockHttpServletResponse();
        new CorsFilter(source).doFilter(request, response, new MockFilterChain());
        return response;
    }

    @Test
    public void defaultConfigRejectsCrossOriginPreflight() throws Exception {
        MockHttpServletResponse response = preflight(source(), "http://localhost:4200");

        assertEquals(HttpServletResponse.SC_FORBIDDEN, response.getStatus());
        assertNull(response.getHeader("Access-Control-Allow-Origin"));
        assertNull(response.getHeader("Access-Control-Allow-Credentials"));
    }

    @Test
    public void configuredOriginGetsCredentialedAccess() throws Exception {
        MockHttpServletResponse response = preflight(source(" https://bank.example.com ", ""), "https://bank.example.com");

        assertEquals(HttpServletResponse.SC_OK, response.getStatus());
        assertEquals("https://bank.example.com", response.getHeader("Access-Control-Allow-Origin"));
        assertEquals("true", response.getHeader("Access-Control-Allow-Credentials"));
    }

    @Test
    public void unlistedOriginIsRejected() throws Exception {
        MockHttpServletResponse response = preflight(source("https://bank.example.com"), "https://evil.example");

        assertEquals(HttpServletResponse.SC_FORBIDDEN, response.getStatus());
        assertNull(response.getHeader("Access-Control-Allow-Origin"));
    }

    @Test
    public void simpleRequestFromUnlistedOriginGetsNoCorsHeaders() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/user/all");
        request.addHeader("Origin", "https://evil.example");
        MockHttpServletResponse response = new MockHttpServletResponse();
        new CorsFilter(source("https://bank.example.com")).doFilter(request, response, new MockFilterChain());

        assertEquals(HttpServletResponse.SC_FORBIDDEN, response.getStatus());
        assertNull(response.getHeader("Access-Control-Allow-Credentials"));
    }

    @Test(expected = IllegalStateException.class)
    public void wildcardOriginIsRefused() {
        source("*");
    }
}
