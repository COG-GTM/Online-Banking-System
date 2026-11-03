package com.userfront.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Collections;

import javax.servlet.Filter;
import javax.servlet.http.Cookie;

import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mock.env.MockPropertySource;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockServletContext;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import com.userfront.dao.UserDao;
import com.userfront.service.UserServiceImpl.UserSecurityService;

public class SecurityConfigLogoutTest {

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

    private static MockMvc mockMvc(MockPropertySource properties) {
        AnnotationConfigWebApplicationContext context = new AnnotationConfigWebApplicationContext();
        context.setServletContext(new MockServletContext());
        context.getEnvironment().getPropertySources().addFirst(properties);
        context.register(TestWebConfig.class, SecurityConfig.class);
        context.refresh();
        Filter springSecurityFilterChain = context.getBean("springSecurityFilterChain", Filter.class);
        return MockMvcBuilders.webAppContextSetup(context).addFilters(springSecurityFilterChain).build();
    }

    private static MockHttpSession authenticatedSession() {
        SecurityContext securityContext = new SecurityContextImpl();
        securityContext.setAuthentication(new UsernamePasswordAuthenticationToken(
                "alice", "n/a", Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))));
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, securityContext);
        session.setAttribute("accountDraft", "pending-transfer");
        return session;
    }

    private static Cookie assertCookieCleared(MockHttpServletResponse response, String name, String path) {
        Cookie cookie = response.getCookie(name);
        assertNotNull("logout must expire the " + name + " cookie", cookie);
        assertEquals(0, cookie.getMaxAge());
        assertNull(cookie.getValue());
        assertEquals(path, cookie.getPath());
        return cookie;
    }

    private static Cookie assertCookieCleared(MockHttpServletResponse response, String name) {
        return assertCookieCleared(response, name, "/");
    }

    @Test
    public void authenticatedSessionCanReachUserFront() throws Exception {
        MockMvc mvc = mockMvc(new MockPropertySource());

        MockHttpServletResponse response = mvc.perform(
                MockMvcRequestBuilders.get("/userFront").session(authenticatedSession()))
                .andReturn().getResponse();

        assertEquals(200, response.getStatus());
        assertEquals("account data", response.getContentAsString());
    }

    @Test
    public void logoutInvalidatesHttpSession() throws Exception {
        MockMvc mvc = mockMvc(new MockPropertySource());
        MockHttpSession session = authenticatedSession();

        MockHttpServletResponse response = mvc.perform(
                MockMvcRequestBuilders.get("/logout").session(session))
                .andReturn().getResponse();

        assertEquals(302, response.getStatus());
        assertEquals("/index?logout", response.getRedirectedUrl());
        assertTrue("logout must invalidate the HTTP session", session.isInvalid());
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    public void logoutExpiresSessionAndRememberMeCookies() throws Exception {
        MockMvc mvc = mockMvc(new MockPropertySource());

        MockHttpServletResponse response = mvc.perform(
                MockMvcRequestBuilders.get("/logout").session(authenticatedSession())
                        .cookie(new Cookie("JSESSIONID", "abc123"), new Cookie("remember-me", "token")))
                .andReturn().getResponse();

        assertCookieCleared(response, "JSESSIONID");
        assertCookieCleared(response, "remember-me");
    }

    @Test
    public void logoutExpiresCustomSessionCookieName() throws Exception {
        MockMvc mvc = mockMvc(new MockPropertySource()
                .withProperty("server.servlet.session.cookie.name", "OBSSESSION"));

        MockHttpServletResponse response = mvc.perform(
                MockMvcRequestBuilders.get("/logout").session(authenticatedSession()))
                .andReturn().getResponse();

        assertCookieCleared(response, "OBSSESSION");
        assertCookieCleared(response, "remember-me");
    }

    @Test
    public void logoutExpiresSessionCookieWithConfiguredPathAndDomain() throws Exception {
        MockMvc mvc = mockMvc(new MockPropertySource()
                .withProperty("server.servlet.session.cookie.path", "/bank")
                .withProperty("server.servlet.session.cookie.domain", "bank.example.com")
                .withProperty("server.servlet.session.cookie.secure", "true"));

        MockHttpServletResponse response = mvc.perform(
                MockMvcRequestBuilders.get("/logout").session(authenticatedSession()))
                .andReturn().getResponse();

        Cookie cookie = assertCookieCleared(response, "JSESSIONID", "/bank");
        assertEquals("bank.example.com", cookie.getDomain());
        assertTrue(cookie.getSecure());
        assertTrue(cookie.isHttpOnly());
    }

    @Test
    public void logoutExpiresSessionCookieOnContextPath() throws Exception {
        MockMvc mvc = mockMvc(new MockPropertySource());

        MockHttpServletResponse response = mvc.perform(
                MockMvcRequestBuilders.get("/obs/logout").contextPath("/obs").session(authenticatedSession()))
                .andReturn().getResponse();

        assertCookieCleared(response, "JSESSIONID", "/obs");
    }
}
