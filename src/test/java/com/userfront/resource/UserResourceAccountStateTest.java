package com.userfront.resource;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
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

@RunWith(SpringJUnit4ClassRunner.class)
@WebAppConfiguration
@ContextConfiguration(classes = UserResourceAccountStateTest.TestConfig.class)
public class UserResourceAccountStateTest {

    @Configuration
    @EnableWebMvc
    @Import({SecurityConfig.class, UserResource.class})
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
            return mock(UserSecurityService.class);
        }
    }

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private UserService userService;

    private MockMvc mockMvc;

    @Before
    public void setUp() {
        reset(userService);
        mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    @Test
    public void adminCanEnableWithPost() throws Exception {
        mockMvc.perform(post("/api/user/alice/enable").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk());
        verify(userService).enableUser("alice");
    }

    @Test
    public void adminCanDisableWithPost() throws Exception {
        mockMvc.perform(post("/api/user/alice/disable").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk());
        verify(userService).disableUser("alice");
    }

    @Test
    public void enableRejectsNonPostMethods() throws Exception {
        mockMvc.perform(get("/api/user/alice/enable").with(user("admin").roles("ADMIN")))
                .andExpect(status().isMethodNotAllowed());
        mockMvc.perform(put("/api/user/alice/enable").with(user("admin").roles("ADMIN")))
                .andExpect(status().isMethodNotAllowed());
        mockMvc.perform(delete("/api/user/alice/enable").with(user("admin").roles("ADMIN")))
                .andExpect(status().isMethodNotAllowed());
        verify(userService, never()).enableUser("alice");
    }

    @Test
    public void disableRejectsNonPostMethods() throws Exception {
        mockMvc.perform(get("/api/user/alice/disable").with(user("admin").roles("ADMIN")))
                .andExpect(status().isMethodNotAllowed());
        mockMvc.perform(put("/api/user/alice/disable").with(user("admin").roles("ADMIN")))
                .andExpect(status().isMethodNotAllowed());
        mockMvc.perform(delete("/api/user/alice/disable").with(user("admin").roles("ADMIN")))
                .andExpect(status().isMethodNotAllowed());
        verify(userService, never()).disableUser("alice");
    }

    @Test
    public void nonAdminCannotEnableOrDisable() throws Exception {
        mockMvc.perform(post("/api/user/alice/enable").with(user("bob").roles("USER")))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/user/alice/disable").with(user("bob").roles("USER")))
                .andExpect(status().isForbidden());
        verify(userService, never()).enableUser("alice");
        verify(userService, never()).disableUser("alice");
    }

    @Test
    public void anonymousCannotEnableOrDisable() throws Exception {
        mockMvc.perform(post("/api/user/alice/enable"))
                .andExpect(status().is3xxRedirection());
        mockMvc.perform(post("/api/user/alice/disable"))
                .andExpect(status().is3xxRedirection());
        verify(userService, never()).enableUser("alice");
        verify(userService, never()).disableUser("alice");
    }
}
