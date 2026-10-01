package com.userfront.resource;

import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Collections;

import javax.servlet.http.Cookie;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import com.userfront.config.SecurityConfig;
import com.userfront.dao.UserDao;
import com.userfront.service.TransactionService;
import com.userfront.service.UserService;
import com.userfront.service.UserServiceImpl.UserSecurityService;

@RunWith(SpringRunner.class)
@WebAppConfiguration
@ContextConfiguration(classes = UserResourceSecurityTest.TestConfig.class)
public class UserResourceSecurityTest {

    @Configuration
    @EnableWebMvc
    @Import(SecurityConfig.class)
    static class TestConfig {

        @Bean
        public UserService userService() {
            return mock(UserService.class);
        }

        @Bean
        public TransactionService transactionService() {
            return mock(TransactionService.class);
        }

        @Bean
        public UserDao userDao() {
            return mock(UserDao.class);
        }

        @Bean
        public UserSecurityService userSecurityService() {
            return new UserSecurityService();
        }

        @Bean
        public UserResource userResource() {
            return new UserResource();
        }
    }

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private FilterChainProxy springSecurityFilterChain;

    @Autowired
    private UserService userService;

    private MockMvc mockMvc;

    private MockHttpSession adminSession;

    @Before
    public void setUp() {
        reset(userService);
        mockMvc = MockMvcBuilders.webAppContextSetup(context).addFilters(springSecurityFilterChain).build();
        UsernamePasswordAuthenticationToken admin = new UsernamePasswordAuthenticationToken("admin", null,
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_ADMIN")));
        adminSession = new MockHttpSession();
        adminSession.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
                new SecurityContextImpl(admin));
    }

    private Cookie csrfCookie() throws Exception {
        Cookie cookie = mockMvc.perform(get("/api/user/all").session(adminSession))
                .andReturn().getResponse().getCookie("XSRF-TOKEN");
        assertNotNull(cookie);
        return cookie;
    }

    @Test
    public void enableRejectsGet() throws Exception {
        mockMvc.perform(get("/api/user/alice/enable").session(adminSession))
                .andExpect(status().isMethodNotAllowed());
        verifyZeroInteractions(userService);
    }

    @Test
    public void disableRejectsGet() throws Exception {
        mockMvc.perform(get("/api/user/alice/disable").session(adminSession))
                .andExpect(status().isMethodNotAllowed());
        verifyZeroInteractions(userService);
    }

    @Test
    public void enableRejectsPostWithoutCsrfToken() throws Exception {
        mockMvc.perform(post("/api/user/alice/enable").session(adminSession))
                .andExpect(status().isForbidden());
        verifyZeroInteractions(userService);
    }

    @Test
    public void disableRejectsPostWithoutCsrfToken() throws Exception {
        mockMvc.perform(post("/api/user/alice/disable").session(adminSession))
                .andExpect(status().isForbidden());
        verifyZeroInteractions(userService);
    }

    @Test
    public void disableRejectsPostWithMismatchedCsrfToken() throws Exception {
        mockMvc.perform(post("/api/user/alice/disable").session(adminSession)
                .cookie(csrfCookie()).header("X-XSRF-TOKEN", "forged"))
                .andExpect(status().isForbidden());
        verify(userService, never()).disableUser(anyString());
    }

    @Test
    public void enableAcceptsPostWithCsrfToken() throws Exception {
        Cookie csrf = csrfCookie();
        mockMvc.perform(post("/api/user/alice/enable").session(adminSession)
                .cookie(csrf).header("X-XSRF-TOKEN", csrf.getValue()))
                .andExpect(status().isOk());
        verify(userService).enableUser("alice");
    }

    @Test
    public void disableAcceptsPostWithCsrfToken() throws Exception {
        Cookie csrf = csrfCookie();
        mockMvc.perform(post("/api/user/alice/disable").session(adminSession)
                .cookie(csrf).header("X-XSRF-TOKEN", csrf.getValue()))
                .andExpect(status().isOk());
        verify(userService).disableUser("alice");
    }
}
