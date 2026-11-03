package com.userfront.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.view.InternalResourceViewResolver;

import com.userfront.dao.RoleDao;
import com.userfront.domain.User;
import com.userfront.service.UserService;

public class HomeControllerSignupTest {

    @Mock
    private UserService userService;

    @Mock
    private RoleDao roleDao;

    @InjectMocks
    private HomeController homeController;

    private MockMvc mockMvc;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        InternalResourceViewResolver viewResolver = new InternalResourceViewResolver();
        viewResolver.setPrefix("/templates/");
        viewResolver.setSuffix(".html");
        mockMvc = MockMvcBuilders.standaloneSetup(homeController).setViewResolvers(viewResolver).build();
    }

    private MvcResult signup(String username, String email) throws Exception {
        return mockMvc.perform(post("/signup")
                .param("firstName", "Ada")
                .param("lastName", "Lovelace")
                .param("phone", "555-0100")
                .param("email", email)
                .param("username", username)
                .param("password", "S3cret-pass"))
                .andReturn();
    }

    private void assertNeutralFailure(MvcResult result) {
        assertEquals("signup", result.getModelAndView().getViewName());
        Map<String, Object> m = result.getModelAndView().getModel();
        assertEquals(Boolean.TRUE, m.get("signupFailed"));
        assertNull(m.get("emailExists"));
        assertNull(m.get("usernameExists"));
        assertNull(((User) m.get("user")).getPassword());
    }

    @Test
    public void takenUsernameReturnsNeutralFailure() throws Exception {
        when(userService.checkUserExists("taken", "new@example.com")).thenReturn(true);
        when(userService.checkUsernameExists("taken")).thenReturn(true);

        assertNeutralFailure(signup("taken", "new@example.com"));
        verify(userService, never()).createUser(any(User.class), any());
    }

    @Test
    public void takenEmailReturnsNeutralFailure() throws Exception {
        when(userService.checkUserExists("fresh", "taken@example.com")).thenReturn(true);
        when(userService.checkEmailExists("taken@example.com")).thenReturn(true);

        assertNeutralFailure(signup("fresh", "taken@example.com"));
        verify(userService, never()).createUser(any(User.class), any());
    }

    @Test
    public void takenUsernameAndEmailReturnNeutralFailure() throws Exception {
        when(userService.checkUserExists(anyString(), anyString())).thenReturn(true);
        when(userService.checkUsernameExists(anyString())).thenReturn(true);
        when(userService.checkEmailExists(anyString())).thenReturn(true);

        assertNeutralFailure(signup("taken", "taken@example.com"));
    }

    @Test
    public void usernameAndEmailCollisionsAreIndistinguishable() throws Exception {
        when(userService.checkUserExists("taken", "a@example.com")).thenReturn(true);
        when(userService.checkUsernameExists("taken")).thenReturn(true);
        when(userService.checkUserExists("b", "taken@example.com")).thenReturn(true);
        when(userService.checkEmailExists("taken@example.com")).thenReturn(true);

        MvcResult byUsername = signup("taken", "a@example.com");
        MvcResult byEmail = signup("b", "taken@example.com");

        assertEquals(byUsername.getModelAndView().getViewName(), byEmail.getModelAndView().getViewName());
        assertEquals(byUsername.getModelAndView().getModel().keySet(), byEmail.getModelAndView().getModel().keySet());
    }

    @Test
    public void newAccountIsCreatedAndRedirected() throws Exception {
        when(userService.checkUserExists(anyString(), anyString())).thenReturn(false);

        mockMvc.perform(post("/signup")
                .param("email", "new@example.com")
                .param("username", "newuser")
                .param("password", "S3cret-pass"))
                .andExpect(redirectedUrl("/"))
                .andExpect(view().name("redirect:/"))
                .andExpect(model().attributeDoesNotExist("signupFailed"));
        verify(userService).createUser(any(User.class), any());
    }
}
