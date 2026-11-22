package com.userfront.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.view.InternalResourceViewResolver;

import com.userfront.dao.RoleDao;
import com.userfront.domain.User;
import com.userfront.domain.security.Role;
import com.userfront.domain.security.UserRole;
import com.userfront.dto.SignupForm;
import com.userfront.service.UserService;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class HomeControllerSignupTest {

    @Mock
    private UserService userService;

    @Mock
    private RoleDao roleDao;

    @InjectMocks
    private HomeController controller;

    private MockMvc mockMvc;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        InternalResourceViewResolver viewResolver = new InternalResourceViewResolver();
        viewResolver.setPrefix("/templates/");
        viewResolver.setSuffix(".html");
        mockMvc = MockMvcBuilders.standaloneSetup(controller).setViewResolvers(viewResolver).build();
        when(roleDao.findByName("ROLE_USER")).thenReturn(new Role());
    }

    private static MockHttpServletRequestBuilder signup(String username, String email, String password) {
        return post("/signup")
                .param("firstName", "Ada")
                .param("lastName", "Lovelace")
                .param("phone", "555-123-4567")
                .param("email", email)
                .param("username", username)
                .param("password", password);
    }

    @Test
    public void validSignupCreatesUserAndRedirects() throws Exception {
        mockMvc.perform(signup("  ada.lovelace ", "ada@example.com", "Analytical1").param("userId", "1").param("enabled", "false"))
                .andExpect(redirectedUrl("/"));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Set<UserRole>> roles = ArgumentCaptor.forClass(Set.class);
        ArgumentCaptor<User> user = ArgumentCaptor.forClass(User.class);
        verify(userService).createUser(user.capture(), roles.capture());
        assertEquals("ada.lovelace", user.getValue().getUsername());
        assertEquals("Analytical1", user.getValue().getPassword());
        assertNull(user.getValue().getUserId());
        assertEquals(true, user.getValue().isEnabled());
        assertEquals(1, roles.getValue().size());
    }

    @Test
    public void invalidEmailIsRejectedServerSide() throws Exception {
        mockMvc.perform(signup("ada.lovelace", "not-an-email", "Analytical1"))
                .andExpect(view().name("signup"))
                .andExpect(model().attributeHasFieldErrors("user", "email"));
        verify(userService, never()).createUser(any(User.class), any());
    }

    @Test
    public void weakPasswordIsRejectedAndNotEchoed() throws Exception {
        SignupForm form = (SignupForm) mockMvc.perform(signup("ada.lovelace", "ada@example.com", "123"))
                .andExpect(view().name("signup"))
                .andExpect(model().attributeHasFieldErrors("user", "password"))
                .andReturn().getModelAndView().getModel().get("user");
        assertNull(form.getPassword());
        verify(userService, never()).createUser(any(User.class), any());
    }

    @Test
    public void invalidUsernameIsRejected() throws Exception {
        mockMvc.perform(signup("<b>x</b>", "ada@example.com", "Analytical1"))
                .andExpect(view().name("signup"))
                .andExpect(model().attributeHasFieldErrors("user", "username"));
        verify(userService, never()).checkUsernameExists(anyString());
        verify(userService, never()).createUser(any(User.class), any());
    }

    @Test
    public void blankFieldsAreRejected() throws Exception {
        mockMvc.perform(post("/signup").param("username", "   ").param("password", ""))
                .andExpect(view().name("signup"))
                .andExpect(model().attributeHasFieldErrors("user", "firstName", "lastName", "phone", "email", "username", "password"));
        verify(userService, never()).createUser(any(User.class), any());
    }

    @Test
    public void duplicateEmailIsReported() throws Exception {
        when(userService.checkEmailExists("ada@example.com")).thenReturn(true);
        mockMvc.perform(signup("ada.lovelace", "ada@example.com", "Analytical1"))
                .andExpect(view().name("signup"))
                .andExpect(model().attribute("emailExists", true));
        verify(userService, never()).createUser(any(User.class), any());
    }
}
