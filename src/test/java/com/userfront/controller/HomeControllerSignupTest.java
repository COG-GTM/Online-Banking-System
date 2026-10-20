package com.userfront.controller;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.view.InternalResourceViewResolver;

import com.userfront.config.SignupRateLimiter;
import com.userfront.dao.RoleDao;
import com.userfront.domain.security.Role;
import com.userfront.service.UserService;

@RunWith(MockitoJUnitRunner.Silent.class)
public class HomeControllerSignupTest {

    @Mock
    private UserService userService;

    @Mock
    private RoleDao roleDao;

    @Mock
    private SignupRateLimiter signupRateLimiter;

    @InjectMocks
    private HomeController homeController;

    private MockMvc mockMvc;

    @Before
    public void setUp() {
        InternalResourceViewResolver viewResolver = new InternalResourceViewResolver();
        viewResolver.setPrefix("/templates/");
        viewResolver.setSuffix(".html");
        mockMvc = MockMvcBuilders.standaloneSetup(homeController).setViewResolvers(viewResolver).build();
        when(signupRateLimiter.tryAcquire(anyString())).thenReturn(true);
        when(roleDao.findByName("ROLE_USER")).thenReturn(new Role());
    }

    private static MockHttpServletRequestBuilder signup(String username, String email) {
        return post("/signup")
                .with(request -> {
                    request.setRemoteAddr("203.0.113.7");
                    return request;
                })
                .param("firstName", "Eve")
                .param("lastName", "Probe")
                .param("phone", "555-000-0000")
                .param("username", username)
                .param("email", email)
                .param("password", "Sup3r-secret!");
    }

    @Test
    public void existingUsernameAndExistingEmailProduceIdenticalResponses() throws Exception {
        when(userService.checkUserExists("alice", "fresh@example.com")).thenReturn(true);
        when(userService.checkUsernameExists("alice")).thenReturn(true);
        when(userService.checkUserExists("fresh", "alice@example.com")).thenReturn(true);
        when(userService.checkEmailExists("alice@example.com")).thenReturn(true);

        MvcResult byUsername = mockMvc.perform(signup("alice", "fresh@example.com"))
                .andExpect(status().isOk())
                .andExpect(view().name("signup"))
                .andExpect(model().attribute("signupUnavailable", true))
                .andExpect(model().attributeDoesNotExist("usernameExists", "emailExists"))
                .andReturn();

        MvcResult byEmail = mockMvc.perform(signup("fresh", "alice@example.com"))
                .andExpect(status().isOk())
                .andExpect(view().name("signup"))
                .andExpect(model().attribute("signupUnavailable", true))
                .andExpect(model().attributeDoesNotExist("usernameExists", "emailExists"))
                .andReturn();

        assertEquals(byUsername.getResponse().getStatus(), byEmail.getResponse().getStatus());
        assertEquals(byUsername.getModelAndView().getModel().keySet(),
                byEmail.getModelAndView().getModel().keySet());
        verify(userService, never()).createUser(any(), any());
    }

    @Test
    public void newAccountIsCreatedAndRedirected() throws Exception {
        when(userService.checkUserExists("fresh", "fresh@example.com")).thenReturn(false);

        mockMvc.perform(signup("fresh", "fresh@example.com"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"));

        verify(userService).createUser(any(), any());
    }

    @Test
    public void rateLimitedClientGets429WithoutAccountLookup() throws Exception {
        when(signupRateLimiter.tryAcquire(eq("203.0.113.7"))).thenReturn(false);

        mockMvc.perform(signup("alice", "alice@example.com"))
                .andExpect(status().isTooManyRequests())
                .andExpect(view().name("signup"))
                .andExpect(model().attribute("signupRateLimited", true))
                .andExpect(model().attributeDoesNotExist("signupUnavailable", "usernameExists", "emailExists"));

        verify(userService, never()).checkUserExists(anyString(), anyString());
        verify(userService, never()).createUser(any(), any());
    }

    @Test
    public void concurrentDuplicateRejectedByDatabaseGetsTheGenericResponse() throws Exception {
        when(userService.checkUserExists("racer", "alice@example.com")).thenReturn(false);
        doThrow(new DataIntegrityViolationException("UK_user_email"))
                .when(userService).createUser(any(), any());

        mockMvc.perform(signup("racer", "alice@example.com"))
                .andExpect(status().isOk())
                .andExpect(view().name("signup"))
                .andExpect(model().attribute("signupUnavailable", true))
                .andExpect(model().attributeDoesNotExist("usernameExists", "emailExists"));
    }
}
