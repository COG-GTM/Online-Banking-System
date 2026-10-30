package com.userfront.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import javax.servlet.Filter;

import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mock.env.MockPropertySource;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import com.userfront.dao.UserDao;
import com.userfront.service.UserServiceImpl.UserSecurityService;

public class SecurityConfigCorsTest {

    private static final String TRUSTED = "https://bank.example.com";
    private static final String UNTRUSTED = "https://evil.example";

    @Configuration
    @EnableWebMvc
    static class TestWebConfig {
        @Bean
        public UserDao userDao() {
            return Mockito.mock(UserDao.class);
        }

        @Bean
        public UserSecurityService userSecurityService() {
            return new UserSecurityService();
        }

        @Bean
        public ProbeController probeController() {
            return new ProbeController();
        }
    }

    @RestController
    static class ProbeController {
        @GetMapping("/userFront")
        public String userFront() {
            return "account data";
        }
    }

    private static MockMvc mockMvc(String allowedOrigins) {
        AnnotationConfigWebApplicationContext context = new AnnotationConfigWebApplicationContext();
        context.setServletContext(new MockServletContext());
        context.getEnvironment().getPropertySources().addFirst(
                new MockPropertySource().withProperty("app.cors.allowed-origins", allowedOrigins));
        context.register(TestWebConfig.class, SecurityConfig.class);
        context.refresh();
        Filter springSecurityFilterChain = context.getBean("springSecurityFilterChain", Filter.class);
        return MockMvcBuilders.webAppContextSetup(context).addFilters(springSecurityFilterChain).build();
    }

    private static MockHttpServletRequestBuilder preflight(String origin) {
        return MockMvcRequestBuilders.options("/userFront")
                .header("Origin", origin)
                .header("Access-Control-Request-Method", "GET");
    }

    private static MockHttpServletResponse perform(MockMvc mvc, MockHttpServletRequestBuilder request) throws Exception {
        return mvc.perform(request).andReturn().getResponse();
    }

    private static void assertNoCorsGrant(MockHttpServletResponse response) {
        assertNull(response.getHeader("Access-Control-Allow-Origin"));
        assertNull(response.getHeader("Access-Control-Allow-Credentials"));
    }

    @Test
    public void defaultConfigGrantsNoCrossOriginAccess() throws Exception {
        MockMvc mvc = mockMvc("");

        MockHttpServletResponse preflight = perform(mvc, preflight("http://localhost:4200"));
        assertEquals(403, preflight.getStatus());
        assertNoCorsGrant(preflight);

        MockHttpServletResponse get = perform(mvc, MockMvcRequestBuilders.get("/userFront").header("Origin", "http://localhost:4200"));
        assertEquals(403, get.getStatus());
        assertNoCorsGrant(get);
    }

    @Test
    public void sameOriginRequestIsUnaffected() throws Exception {
        MockHttpServletResponse response = perform(mockMvc(""),
                MockMvcRequestBuilders.get("http://localhost/userFront").header("Origin", "http://localhost"));

        assertEquals(302, response.getStatus());
        assertTrue(response.getRedirectedUrl().endsWith("/index"));
        assertNoCorsGrant(response);
    }

    @Test
    public void optionsWithoutOriginGoesThroughAuthentication() throws Exception {
        MockHttpServletResponse response = perform(mockMvc(TRUSTED), MockMvcRequestBuilders.options("/userFront"));

        assertEquals(302, response.getStatus());
        assertTrue(response.getRedirectedUrl().endsWith("/index"));
    }

    @Test
    public void configuredOriginGetsCredentialedPreflight() throws Exception {
        MockHttpServletResponse response = perform(mockMvc(" " + TRUSTED + " , "), preflight(TRUSTED));

        assertEquals(200, response.getStatus());
        assertEquals(TRUSTED, response.getHeader("Access-Control-Allow-Origin"));
        assertEquals("true", response.getHeader("Access-Control-Allow-Credentials"));
    }

    @Test
    public void configuredOriginStillNeedsAuthenticationForActualRequest() throws Exception {
        MockHttpServletResponse response = perform(mockMvc(TRUSTED),
                MockMvcRequestBuilders.get("/userFront").header("Origin", TRUSTED));

        assertEquals(302, response.getStatus());
        assertTrue(response.getRedirectedUrl().endsWith("/index"));
    }

    @Test
    public void unlistedOriginIsRejected() throws Exception {
        MockMvc mvc = mockMvc(TRUSTED);

        MockHttpServletResponse preflight = perform(mvc, preflight(UNTRUSTED));
        assertEquals(403, preflight.getStatus());
        assertNoCorsGrant(preflight);

        MockHttpServletResponse get = perform(mvc, MockMvcRequestBuilders.get("/userFront").header("Origin", UNTRUSTED));
        assertEquals(403, get.getStatus());
        assertNoCorsGrant(get);
    }

    @Test
    public void parseAllowedOriginsTrimsAndSkipsBlanks() {
        assertEquals(java.util.Arrays.asList(TRUSTED, "http://localhost:4200"),
                SecurityConfig.parseAllowedOrigins(" " + TRUSTED + ",, http://localhost:4200 "));
        assertTrue(SecurityConfig.parseAllowedOrigins("").isEmpty());
        assertTrue(SecurityConfig.parseAllowedOrigins(null).isEmpty());
    }

    @Test
    public void wildcardAndMalformedOriginsAreRefused() {
        String[] invalid = {"*", "null", "https://*.example.com", "bank.example.com", "ftp://bank.example.com",
                "https://bank.example.com/app", "https://user@bank.example.com"};
        for (String origin : invalid) {
            try {
                SecurityConfig.parseAllowedOrigins(origin);
                fail("expected rejection of " + origin);
            } catch (IllegalStateException expected) {
                // expected
            }
        }
    }
}
