package com.userfront.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.time.Clock;
import java.time.Duration;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.view.InternalResourceViewResolver;

import com.userfront.dao.RoleDao;
import com.userfront.security.SignupRateLimiter;
import com.userfront.service.UserService;

@RunWith(MockitoJUnitRunner.Silent.class)
public class HomeControllerSignupTest {

    private static final int MAX_ATTEMPTS = 5;

    @Mock
    private UserService userService;

    @Mock
    private RoleDao roleDao;

    private MockMvc mockMvc;

    @Before
    public void setUp() {
        HomeController controller = new HomeController();
        ReflectionTestUtils.setField(controller, "userService", userService);
        ReflectionTestUtils.setField(controller, "roleDao", roleDao);
        ReflectionTestUtils.setField(controller, "signupRateLimiter",
                new SignupRateLimiter(MAX_ATTEMPTS, Duration.ofMinutes(15), Clock.systemUTC()));

        InternalResourceViewResolver viewResolver = new InternalResourceViewResolver();
        viewResolver.setPrefix("/templates/");
        viewResolver.setSuffix(".html");

        mockMvc = MockMvcBuilders.standaloneSetup(controller).setViewResolvers(viewResolver).build();

        when(userService.checkUserExists("alice", "alice@example.com")).thenReturn(true);
        when(userService.checkUsernameExists("alice")).thenReturn(true);
        when(userService.checkEmailExists("alice@example.com")).thenReturn(true);
    }

    private MockHttpServletRequestBuilder signup(String username, String email, String remoteAddr) {
        return post("/signup")
                .param("username", username)
                .param("email", email)
                .param("password", "secret")
                .with(request -> {
                    request.setRemoteAddr(remoteAddr);
                    return request;
                });
    }

    @Test
    public void existingUsernameReturnsGenericErrorWithoutRevealingWhichFieldExists() throws Exception {
        when(userService.checkUserExists("alice", "new@example.com")).thenReturn(true);

        mockMvc.perform(signup("alice", "new@example.com", "10.0.0.1"))
                .andExpect(status().isOk())
                .andExpect(view().name("signup"))
                .andExpect(model().attribute("signupFailed", true))
                .andExpect(model().attributeDoesNotExist("usernameExists", "emailExists"));

        verify(userService, never()).createUser(any(), any());
    }

    @Test
    public void existingEmailReturnsSameGenericErrorAsExistingUsername() throws Exception {
        when(userService.checkUserExists("newuser", "alice@example.com")).thenReturn(true);

        mockMvc.perform(signup("newuser", "alice@example.com", "10.0.0.2"))
                .andExpect(status().isOk())
                .andExpect(view().name("signup"))
                .andExpect(model().attribute("signupFailed", true))
                .andExpect(model().attributeDoesNotExist("usernameExists", "emailExists"));

        verify(userService, never()).createUser(any(), any());
    }

    @Test
    public void newUserIsCreatedAndRedirected() throws Exception {
        mockMvc.perform(signup("bob", "bob@example.com", "10.0.0.3"))
                .andExpect(redirectedUrl("/"));

        verify(userService).createUser(any(), any());
    }

    @Test
    public void repeatedSignupAttemptsFromSameClientAreThrottled() throws Exception {
        for (int i = 0; i < MAX_ATTEMPTS; i++) {
            mockMvc.perform(signup("probe" + i, "probe" + i + "@example.com", "10.0.0.4"))
                    .andExpect(status().is3xxRedirection());
        }

        mockMvc.perform(signup("alice", "alice@example.com", "10.0.0.4"))
                .andExpect(status().isTooManyRequests())
                .andExpect(view().name("signup"))
                .andExpect(model().attribute("signupThrottled", true))
                .andExpect(model().attributeDoesNotExist("signupFailed", "usernameExists", "emailExists"));

        verify(userService, never()).checkUserExists("alice", "alice@example.com");

        mockMvc.perform(signup("carol", "carol@example.com", "10.0.0.5"))
                .andExpect(redirectedUrl("/"));
    }
}
