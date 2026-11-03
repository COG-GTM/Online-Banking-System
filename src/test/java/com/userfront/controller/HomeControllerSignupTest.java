package com.userfront.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
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
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.view.InternalResourceViewResolver;

import com.userfront.domain.SignupForm;
import com.userfront.service.UserService;

@RunWith(MockitoJUnitRunner.class)
public class HomeControllerSignupTest {

    @Mock
    private UserService userService;

    @InjectMocks
    private HomeController homeController;

    private MockMvc mockMvc;

    @Before
    public void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(homeController)
                .setViewResolvers(new InternalResourceViewResolver("/WEB-INF/views/", ".html"))
                .build();
    }

    @Test
    public void signupBindsOnlyFormFieldsAndIgnoresEntityParameters() throws Exception {
        when(userService.checkUserExists(any(), any())).thenReturn(false);

        mockMvc.perform(post("/signup")
                .param("username", "alice")
                .param("password", "S3cure!Passw0rd")
                .param("firstName", "Alice")
                .param("lastName", "Doe")
                .param("email", "alice@example.com")
                .param("phone", "555-123-4567")
                .param("userId", "1")
                .param("enabled", "false")
                .param("userRoles[0].role.name", "ROLE_ADMIN")
                .param("primaryAccount.id", "1")
                .param("savingsAccount.id", "1"))
                .andExpect(redirectedUrl("/"));

        ArgumentCaptor<SignupForm> captor = ArgumentCaptor.forClass(SignupForm.class);
        verify(userService).registerUser(captor.capture());
        SignupForm form = captor.getValue();
        assertEquals("alice", form.getUsername());
        assertEquals("S3cure!Passw0rd", form.getPassword());
        assertEquals("Alice", form.getFirstName());
        assertEquals("Doe", form.getLastName());
        assertEquals("alice@example.com", form.getEmail());
        assertEquals("555-123-4567", form.getPhone());
    }

    @Test
    public void duplicateSignupRedisplaysFormWithoutPasswordAndDoesNotRegister() throws Exception {
        when(userService.checkUserExists(any(), any())).thenReturn(true);
        when(userService.checkUsernameExists("alice")).thenReturn(true);

        MvcResult result = mockMvc.perform(post("/signup")
                .param("username", "alice")
                .param("password", "S3cure!Passw0rd")
                .param("email", "alice@example.com")
                .param("userId", "1"))
                .andExpect(view().name("signup"))
                .andExpect(model().attribute("usernameExists", true))
                .andReturn();

        SignupForm form = (SignupForm) result.getModelAndView().getModel().get("user");
        assertEquals("alice", form.getUsername());
        assertNull(form.getPassword());
        verify(userService, never()).registerUser(any());
    }
}
