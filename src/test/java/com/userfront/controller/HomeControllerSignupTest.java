package com.userfront.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
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
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.view.InternalResourceViewResolver;

import com.userfront.dao.RoleDao;
import com.userfront.domain.User;
import com.userfront.domain.security.Role;
import com.userfront.domain.security.UserRole;
import com.userfront.service.UserService;

public class HomeControllerSignupTest {

    @Mock
    private UserService userService;

    @Mock
    private RoleDao roleDao;

    @InjectMocks
    private HomeController homeController;

    private MockMvc mockMvc;

    private Role userRole;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mockMvc = MockMvcBuilders.standaloneSetup(homeController)
                .setViewResolvers(new InternalResourceViewResolver("/templates/", ".html"))
                .build();
        userRole = new Role();
        userRole.setRoleId(1);
        userRole.setName("ROLE_USER");
        when(roleDao.findByName("ROLE_USER")).thenReturn(userRole);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void signupIgnoresFieldsOutsideTheSignupForm() throws Exception {
        mockMvc.perform(post("/signup")
                .param("firstName", "Mallory")
                .param("lastName", "Attacker")
                .param("phone", "555-555-5555")
                .param("email", "mallory@example.com")
                .param("username", "mallory")
                .param("password", "s3cret")
                .param("userId", "1")
                .param("enabled", "false")
                .param("primaryAccount.id", "1")
                .param("primaryAccount.accountBalance", "1000000")
                .param("savingsAccount.id", "1")
                .param("recipientList[0].name", "injected")
                .param("recipientList[0].accountNumber", "123")
                .param("appointmentList[0].location", "injected")
                .param("appointmentList[0].confirmed", "true"))
                .andExpect(redirectedUrl("/"));

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        ArgumentCaptor<Set> rolesCaptor = ArgumentCaptor.forClass(Set.class);
        verify(userService).createUser(userCaptor.capture(), rolesCaptor.capture());

        User created = userCaptor.getValue();
        assertEquals("Mallory", created.getFirstName());
        assertEquals("Attacker", created.getLastName());
        assertEquals("555-555-5555", created.getPhone());
        assertEquals("mallory@example.com", created.getEmail());
        assertEquals("mallory", created.getUsername());
        assertEquals("s3cret", created.getPassword());

        assertNull(created.getUserId());
        assertTrue(created.isEnabled());
        assertNull(created.getPrimaryAccount());
        assertNull(created.getSavingsAccount());
        assertNull(created.getRecipientList());
        assertNull(created.getAppointmentList());
        assertTrue(created.getUserRoles().isEmpty());

        Set<UserRole> roles = rolesCaptor.getValue();
        assertEquals(1, roles.size());
        UserRole granted = roles.iterator().next();
        assertEquals("ROLE_USER", granted.getRole().getName());
        assertEquals(created, granted.getUser());
    }

    @Test
    public void duplicateSignupRerendersFormWithoutCreatingUser() throws Exception {
        when(userService.checkUserExists("taken", "taken@example.com")).thenReturn(true);
        when(userService.checkUsernameExists("taken")).thenReturn(true);

        mockMvc.perform(post("/signup")
                .param("username", "taken")
                .param("email", "taken@example.com")
                .param("password", "pw")
                .param("userId", "1"))
                .andExpect(view().name("signup"))
                .andExpect(model().attribute("usernameExists", true))
                .andExpect(model().attributeExists("user"));

        verify(userService, never()).createUser(any(User.class), any());
    }
}
