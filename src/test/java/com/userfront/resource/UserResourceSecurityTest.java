package com.userfront.resource;

import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
    @Import({SecurityConfig.class, UserResource.class, CsrfResource.class})
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

    private static final RequestPostProcessor ADMIN = user("admin").roles("ADMIN");

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private UserService userService;

    private MockMvc mvc;

    @Before
    public void setUp() {
        reset(userService);
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    @Test
    public void getEnableIsRejectedAndDoesNotChangeAccount() throws Exception {
        mvc.perform(get("/api/user/alice/enable").with(ADMIN)).andExpect(status().isMethodNotAllowed());
        verify(userService, never()).enableUser(anyString());
    }

    @Test
    public void getDisableIsRejectedAndDoesNotChangeAccount() throws Exception {
        mvc.perform(get("/api/user/alice/disable").with(ADMIN)).andExpect(status().isMethodNotAllowed());
        verify(userService, never()).disableUser(anyString());
    }

    @Test
    public void postWithoutCsrfTokenIsForbidden() throws Exception {
        mvc.perform(post("/api/user/alice/enable").with(ADMIN)).andExpect(status().isForbidden());
        mvc.perform(post("/api/user/alice/disable").with(ADMIN)).andExpect(status().isForbidden());
        verify(userService, never()).enableUser(anyString());
        verify(userService, never()).disableUser(anyString());
    }

    @Test
    public void postWithInvalidCsrfTokenIsForbidden() throws Exception {
        mvc.perform(post("/api/user/alice/disable").with(ADMIN).with(csrf().useInvalidToken()))
                .andExpect(status().isForbidden());
        verify(userService, never()).disableUser(anyString());
    }

    @Test
    public void adminPostWithCsrfTokenEnablesAndDisables() throws Exception {
        mvc.perform(post("/api/user/alice/enable").with(ADMIN).with(csrf())).andExpect(status().isOk());
        verify(userService).enableUser("alice");

        mvc.perform(post("/api/user/alice/disable").with(ADMIN).with(csrf())).andExpect(status().isOk());
        verify(userService).disableUser("alice");
    }

    @Test
    public void nonAdminCannotToggleAccounts() throws Exception {
        mvc.perform(post("/api/user/alice/disable").with(user("bob").roles("USER")).with(csrf()))
                .andExpect(status().isForbidden());
        verify(userService, never()).disableUser(anyString());
    }

    @Test
    public void csrfEndpointIssuesSessionTokenAcceptedInHeader() throws Exception {
        MockHttpSession session = new MockHttpSession();
        MvcResult result = mvc.perform(get("/api/csrf").session(session).with(ADMIN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.headerName").value("X-CSRF-TOKEN"))
                .andExpect(jsonPath("$.token", notNullValue()))
                .andReturn();
        JsonNode body = new ObjectMapper().readTree(result.getResponse().getContentAsString());

        mvc.perform(post("/api/user/alice/disable").session(session).with(ADMIN)
                .header(body.get("headerName").asText(), body.get("token").asText()))
                .andExpect(status().isOk());
        verify(userService).disableUser("alice");
    }
}
