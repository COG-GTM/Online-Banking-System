package com.userfront.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.security.Principal;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.view.InternalResourceViewResolver;
import org.springframework.web.util.NestedServletException;

import com.userfront.domain.User;
import com.userfront.service.UserService;

@RunWith(MockitoJUnitRunner.class)
public class UserControllerTest {

    @Mock
    private UserService userService;

    @InjectMocks
    private UserController userController;

    private MockMvc mockMvc;

    private User alice;
    private User bob;

    @Before
    public void setUp() {
        InternalResourceViewResolver viewResolver = new InternalResourceViewResolver();
        viewResolver.setPrefix("/templates/");
        viewResolver.setSuffix(".html");
        mockMvc = MockMvcBuilders.standaloneSetup(userController).setViewResolvers(viewResolver).build();

        alice = user(1L, "alice", "alice@example.com");
        bob = user(2L, "bob", "bob@example.com");
    }

    @Test
    public void submittedUsernameCannotRetargetTheUpdate() throws Exception {
        when(userService.findByUsername(anyString()))
                .thenAnswer(inv -> "bob".equals(inv.getArgument(0)) ? bob : alice);

        mockMvc.perform(post("/user/profile").principal(principal("alice"))
                .param("username", "bob")
                .param("firstName", "Mallory")
                .param("lastName", "M")
                .param("email", "mallory@evil.example")
                .param("phone", "555-0100"))
                .andExpect(status().isOk())
                .andExpect(view().name("profile"))
                .andExpect(model().attribute("user", alice));

        verify(userService, never()).findByUsername("bob");
        verify(userService, never()).saveUser(bob);
        assertEquals("bob@example.com", bob.getEmail());
        assertNull(bob.getPhone());
        assertNull(bob.getFirstName());
    }

    @Test
    public void updatesOnlyContactFieldsOfThePrincipal() throws Exception {
        when(userService.findByUsername("alice")).thenReturn(alice);

        mockMvc.perform(post("/user/profile").principal(principal("alice"))
                .param("id", "2")
                .param("userId", "2")
                .param("username", "bob")
                .param("password", "pwned")
                .param("enabled", "false")
                .param("firstName", "Alicia")
                .param("lastName", "Smith")
                .param("email", "alicia@example.com")
                .param("phone", "555-0100"))
                .andExpect(status().isOk())
                .andExpect(view().name("profile"));

        verify(userService).saveUser(alice);
        assertEquals(Long.valueOf(1L), alice.getUserId());
        assertEquals("alice", alice.getUsername());
        assertEquals("secret", alice.getPassword());
        assertEquals(true, alice.isEnabled());
        assertEquals("Alicia", alice.getFirstName());
        assertEquals("Smith", alice.getLastName());
        assertEquals("alicia@example.com", alice.getEmail());
        assertEquals("555-0100", alice.getPhone());
    }

    @Test
    public void rejectsEmailOwnedByAnotherUser() throws Exception {
        when(userService.findByUsername("alice")).thenReturn(alice);
        when(userService.findByEmail("bob@example.com")).thenReturn(bob);

        mockMvc.perform(post("/user/profile").principal(principal("alice"))
                .param("firstName", "Alicia")
                .param("lastName", "Smith")
                .param("email", "bob@example.com")
                .param("phone", "555-0100"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("emailExists", true))
                .andExpect(model().attribute("user", alice));

        verify(userService, never()).saveUser(any(User.class));
        assertEquals("alice@example.com", alice.getEmail());
        assertNull(alice.getFirstName());
    }

    @Test
    public void restoresPrincipalWhenSaveHitsUniqueEmail() throws Exception {
        when(userService.findByUsername("alice")).thenReturn(alice);
        when(userService.saveUser(alice)).thenThrow(new DataIntegrityViolationException("uk_email"));
        when(userService.findByEmail("shared@example.com")).thenReturn(null, bob);

        mockMvc.perform(post("/user/profile").principal(principal("alice"))
                .param("firstName", "Alicia")
                .param("lastName", "Smith")
                .param("email", "shared@example.com")
                .param("phone", "555-0100"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("emailExists", true));

        assertEquals("alice@example.com", alice.getEmail());
        assertNull(alice.getFirstName());
    }

    @Test
    public void otherIntegrityFailuresAreNotReportedAsDuplicateEmail() throws Exception {
        when(userService.findByUsername("alice")).thenReturn(alice);
        when(userService.saveUser(alice)).thenThrow(new DataIntegrityViolationException("first_name too long"));

        try {
            mockMvc.perform(post("/user/profile").principal(principal("alice"))
                    .param("firstName", "Alicia")
                    .param("lastName", "Smith")
                    .param("email", "alicia@example.com")
                    .param("phone", "555-0100"));
            fail("expected the integrity violation to propagate");
        } catch (NestedServletException e) {
            assertTrue(e.getCause() instanceof DataIntegrityViolationException);
        }

        assertEquals("alice@example.com", alice.getEmail());
        assertNull(alice.getFirstName());
    }

    private static Principal principal(String name) {
        return () -> name;
    }

    private static User user(Long id, String username, String email) {
        User user = new User();
        user.setUserId(id);
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword("secret");
        user.setEnabled(true);
        return user;
    }
}
