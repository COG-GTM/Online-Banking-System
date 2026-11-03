package com.userfront.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import javax.servlet.http.Cookie;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import com.userfront.dao.UserDao;
import com.userfront.domain.User;
import com.userfront.service.UserServiceImpl.UserSecurityService;

@RunWith(SpringRunner.class)
@WebAppConfiguration
@ContextConfiguration(classes = {SecurityConfig.class, SessionManagementTest.TestBeans.class})
public class SessionManagementTest {

    private static final String USERNAME = "alice";
    private static final String PASSWORD = "correct-horse";

    @Configuration
    @EnableWebMvc
    static class TestBeans {
        @Bean
        public UserSecurityService userSecurityService() {
            return new UserSecurityService();
        }

        @Bean
        public UserDao userDao() {
            UserDao userDao = mock(UserDao.class);
            String hash = new BCryptPasswordEncoder(4).encode(PASSWORD);
            when(userDao.findByUsername(anyString())).thenAnswer(invocation -> {
                User user = new User();
                user.setUserId(1L);
                user.setUsername(invocation.getArgument(0));
                user.setPassword(hash);
                return user;
            });
            return userDao;
        }
    }

    @Autowired
    private WebApplicationContext context;

    private MockMvc mvc;

    @Before
    public void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    private MockHttpSession login(MockHttpSession session) throws Exception {
        MvcResult result = mvc.perform(post("/index").session(session)
                .param("username", USERNAME).param("password", PASSWORD))
                .andExpect(redirectedUrl("/userFront"))
                .andReturn();
        MockHttpSession authenticated = (MockHttpSession) result.getRequest().getSession(false);
        assertNotNull(authenticated);
        return authenticated;
    }

    @Test
    public void loginMigratesToNewSessionAndInvalidatesPreLoginSession() throws Exception {
        MockHttpSession preLogin = new MockHttpSession();
        preLogin.setAttribute("carried", "over");
        String preLoginId = preLogin.getId();

        MockHttpSession authenticated = login(preLogin);

        assertNotEquals(preLoginId, authenticated.getId());
        assertTrue("pre-login session must be invalidated", preLogin.isInvalid());
        assertEquals("over", authenticated.getAttribute("carried"));
    }

    @Test
    public void secondLoginExpiresFirstSession() throws Exception {
        MockHttpSession first = login(new MockHttpSession());
        mvc.perform(get("/userFront").session(first)).andExpect(status().isNotFound());

        MockHttpSession second = login(new MockHttpSession());

        mvc.perform(get("/userFront").session(first)).andExpect(redirectedUrl("/index?expired"));
        mvc.perform(get("/userFront").session(second)).andExpect(status().isNotFound());
    }

    @Test
    public void rememberMeCookieIsClearedOnExpiryAndRememberMeLoginStillCountsTowardsLimit() throws Exception {
        MvcResult first = mvc.perform(post("/index").session(new MockHttpSession())
                .param("username", USERNAME).param("password", PASSWORD).param("remember-me", "on"))
                .andExpect(redirectedUrl("/userFront"))
                .andReturn();
        MockHttpSession firstSession = (MockHttpSession) first.getRequest().getSession(false);
        Cookie rememberMe = first.getResponse().getCookie("remember-me");
        assertNotNull(rememberMe);

        MockHttpSession second = login(new MockHttpSession());

        MvcResult expired = mvc.perform(get("/userFront").session(firstSession).cookie(rememberMe))
                .andExpect(redirectedUrl("/index?expired"))
                .andReturn();
        Cookie cleared = expired.getResponse().getCookie("remember-me");
        assertNotNull(cleared);
        assertEquals(0, cleared.getMaxAge());

        MvcResult restored = mvc.perform(get("/userFront").session(new MockHttpSession()).cookie(rememberMe))
                .andExpect(status().isNotFound())
                .andReturn();
        MockHttpSession restoredSession = (MockHttpSession) restored.getRequest().getSession(false);

        mvc.perform(get("/userFront").session(second)).andExpect(redirectedUrl("/index?expired"));
        mvc.perform(get("/userFront").session(restoredSession)).andExpect(status().isNotFound());
    }

    @Test
    public void expiredLandingPageIsPublic() throws Exception {
        mvc.perform(get("/index?expired")).andExpect(status().isNotFound());
    }

    @Test
    public void usersAreIdentifiedByUserIdForSessionRegistry() {
        User a = new User();
        a.setUserId(1L);
        a.setUsername(USERNAME);
        User b = new User();
        b.setUserId(1L);
        b.setUsername(USERNAME);
        User sameNameOtherAccount = new User();
        sameNameOtherAccount.setUserId(2L);
        sameNameOtherAccount.setUsername(USERNAME);

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
        assertNotEquals(a, sameNameOtherAccount);
        assertNotEquals(new User(), new User());
    }
}
