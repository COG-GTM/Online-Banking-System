package com.userfront.controller;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.view.InternalResourceViewResolver;

import com.userfront.dto.SignupForm;
import com.userfront.service.UserService;

@RunWith(MockitoJUnitRunner.Silent.class)
public class HomeControllerTest {

    @Mock
    private UserService userService;

    @InjectMocks
    private HomeController homeController;

    private MockMvc mockMvc;

    @Before
    public void setUp() {
        InternalResourceViewResolver viewResolver = new InternalResourceViewResolver();
        viewResolver.setPrefix("/templates/");
        viewResolver.setSuffix(".html");
        mockMvc = MockMvcBuilders.standaloneSetup(homeController).setViewResolvers(viewResolver).build();
        when(userService.checkUserExists(any(), any())).thenReturn(false);
    }

    @Test
    public void rejectsShortPassword() throws Exception {
        mockMvc.perform(signup("short", "alice@example.com"))
                .andExpect(view().name("signup"))
                .andExpect(model().attributeHasFieldErrors("user", "password"));

        verify(userService, never()).registerUser(any());
    }

    @Test
    public void rejectsInvalidEmail() throws Exception {
        mockMvc.perform(signup("correct-horse-battery", "not-an-email"))
                .andExpect(view().name("signup"))
                .andExpect(model().attributeHasFieldErrors("user", "email"));

        verify(userService, never()).registerUser(any());
    }

    @Test
    public void registersValidSignupIgnoringNonFormFields() throws Exception {
        mockMvc.perform(validSignup()
                        .param("userId", "1")
                        .param("enabled", "false")
                        .param("userRoles[0].role.name", "ROLE_ADMIN")
                        .param("primaryAccount.accountBalance", "1000000"))
                .andExpect(redirectedUrl("/"));

        ArgumentCaptor<SignupForm> captor = ArgumentCaptor.forClass(SignupForm.class);
        verify(userService).registerUser(captor.capture());
        SignupForm form = captor.getValue();
        assertEquals("alice", form.getUsername());
        assertEquals("correct-horse-battery", form.getPassword());
        assertEquals("alice@example.com", form.getEmail());
    }

    private MockHttpServletRequestBuilder validSignup() {
        return signup("correct-horse-battery", "alice@example.com");
    }

    private MockHttpServletRequestBuilder signup(String password, String email) {
        return post("/signup")
                .param("username", "alice")
                .param("password", password)
                .param("firstName", "Alice")
                .param("lastName", "Smith")
                .param("email", email)
                .param("phone", "555-123-4567");
    }
}
