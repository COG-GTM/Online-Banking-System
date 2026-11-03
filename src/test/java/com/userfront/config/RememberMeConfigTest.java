package com.userfront.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;

import javax.servlet.Filter;
import javax.servlet.http.Cookie;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import com.userfront.dao.UserDao;
import com.userfront.domain.User;
import com.userfront.service.UserServiceImpl.UserSecurityService;

@RunWith(Enclosed.class)
public class RememberMeConfigTest {

    static final String KEY = "0123456789abcdef0123456789abcdef-test-only";

    @Configuration
    @EnableWebMvc
    @Import({SecurityConfig.class, RememberMeProperties.class, UserSecurityService.class})
    static class TestConfig {

        @Bean
        UserDao userDao() {
            return mock(UserDao.class);
        }
    }

    abstract static class SecurityTestBase {

        @Autowired
        WebApplicationContext context;

        @Autowired
        UserDao userDao;

        @Autowired
        BCryptPasswordEncoder passwordEncoder;

        MockMvc mvc;

        @Before
        public void setUp() {
            User alice = new User();
            alice.setUsername("alice");
            alice.setPassword(passwordEncoder.encode("s3cret"));
            when(userDao.findByUsername("alice")).thenReturn(alice);
            mvc = MockMvcBuilders.webAppContextSetup(context)
                    .addFilters(context.getBean("springSecurityFilterChain", Filter.class))
                    .build();
        }

        MvcResult loginWithRememberMe() throws Exception {
            return mvc.perform(post("/index")
                    .param("username", "alice")
                    .param("password", "s3cret")
                    .param("remember-me", "on"))
                    .andExpect(redirectedUrl("/userFront"))
                    .andReturn();
        }
    }

    @RunWith(SpringRunner.class)
    @WebAppConfiguration
    @ContextConfiguration(classes = TestConfig.class)
    @TestPropertySource(properties = "app.security.remember-me.key=" + KEY)
    public static class WhenKeyConfigured extends SecurityTestBase {

        @Test
        public void cookieUsesConfiguredLifetimeAndSecureHttpOnlyFlags() throws Exception {
            Cookie cookie = loginWithRememberMe().getResponse().getCookie(SecurityConfig.REMEMBER_ME_COOKIE);

            assertNotNull(cookie);
            assertEquals(86400, cookie.getMaxAge());
            assertTrue(cookie.getSecure());
            assertTrue(cookie.isHttpOnly());
        }

        @Test
        public void validCookieAuthenticatesNewSession() throws Exception {
            Cookie cookie = loginWithRememberMe().getResponse().getCookie(SecurityConfig.REMEMBER_ME_COOKIE);

            MvcResult result = mvc.perform(get("/userFront").cookie(cookie)).andReturn();

            assertFalse(String.valueOf(result.getResponse().getRedirectedUrl()).endsWith("/index"));
        }

        @Test
        public void tamperedCookieIsRejected() throws Exception {
            Cookie forged = new Cookie(SecurityConfig.REMEMBER_ME_COOKIE,
                    java.util.Base64.getEncoder().encodeToString(
                            ("alice:" + (System.currentTimeMillis() + 100000) + ":00000000000000000000000000000000")
                                    .getBytes("UTF-8")));

            mvc.perform(get("/userFront").cookie(forged))
                    .andExpect(redirectedUrl("http://localhost/index"));
        }
    }

    @RunWith(SpringRunner.class)
    @WebAppConfiguration
    @ContextConfiguration(classes = TestConfig.class)
    @TestPropertySource(properties = "app.security.remember-me.key=")
    public static class WhenKeyMissing extends SecurityTestBase {

        @Test
        public void rememberMeIsDisabled() throws Exception {
            assertNull(loginWithRememberMe().getResponse().getCookie(SecurityConfig.REMEMBER_ME_COOKIE));
        }
    }

    public static class PropertiesValidation {

        @Test
        public void rejectsShortKey() {
            assertInvalid(properties("too-short", 86400));
        }

        @Test
        public void rejectsNonPositiveOrExcessiveLifetime() {
            assertInvalid(properties(KEY, 0));
            assertInvalid(properties(KEY, RememberMeProperties.MAX_TOKEN_VALIDITY_SECONDS + 1));
        }

        @Test
        public void acceptsMissingKeyAsDisabled() {
            RememberMeProperties properties = properties("", 86400);
            properties.validate();
            assertFalse(properties.isEnabled());
        }

        private static RememberMeProperties properties(String key, int validity) {
            RememberMeProperties properties = new RememberMeProperties();
            ReflectionTestUtils.setField(properties, "key", key);
            ReflectionTestUtils.setField(properties, "tokenValiditySeconds", validity);
            ReflectionTestUtils.setField(properties, "secureCookie", true);
            return properties;
        }

        private static void assertInvalid(RememberMeProperties properties) {
            try {
                properties.validate();
                fail("expected IllegalStateException");
            } catch (IllegalStateException expected) {
                // expected
            }
        }
    }
}
