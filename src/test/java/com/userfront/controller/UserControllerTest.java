package com.userfront.controller;

import static org.junit.Assert.assertEquals;
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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.view.InternalResourceViewResolver;

import com.userfront.domain.User;
import com.userfront.service.UserService;

public class UserControllerTest {

    @Mock
    private UserService userService;

    @InjectMocks
    private UserController userController;

    private MockMvc mockMvc;

    private User alice;
    private User bob;

    private final Principal alicePrincipal = new Principal() {
        @Override
        public String getName() {
            return "alice";
        }
    };

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mockMvc = MockMvcBuilders.standaloneSetup(userController)
                .setViewResolvers(new InternalResourceViewResolver("/WEB-INF/", ".html"))
                .build();

        alice = user(1L, "alice", "Alice", "Smith", "alice@example.com", "111", "alicehash");
        bob = user(2L, "bob", "Bob", "Jones", "bob@example.com", "222", "bobhash");
        when(userService.findByUsername("alice")).thenReturn(alice);
        when(userService.findByUsername("bob")).thenReturn(bob);
        when(userService.findByEmail("alice@example.com")).thenReturn(alice);
        when(userService.findByEmail("bob@example.com")).thenReturn(bob);
    }

    @Test
    public void updatesOnlyThePrincipalEvenWhenAnotherUsernameIsSubmitted() throws Exception {
        mockMvc.perform(post("/user/profile").principal(alicePrincipal)
                .param("username", "bob")
                .param("firstName", "Mallory")
                .param("lastName", "Evil")
                .param("email", "alice@new.example.com")
                .param("phone", "999"))
                .andExpect(status().isOk())
                .andExpect(view().name("profile"))
                .andExpect(model().attribute("user", alice));

        assertEquals("alice", alice.getUsername());
        assertEquals("Mallory", alice.getFirstName());
        assertEquals("Evil", alice.getLastName());
        assertEquals("alice@new.example.com", alice.getEmail());
        assertEquals("999", alice.getPhone());
        verify(userService).saveUser(alice);

        assertEquals("Bob", bob.getFirstName());
        assertEquals("bob@example.com", bob.getEmail());
        verify(userService, never()).saveUser(bob);
    }

    @Test
    public void ignoresNonProfileFields() throws Exception {
        mockMvc.perform(post("/user/profile").principal(alicePrincipal)
                .param("userId", "2")
                .param("id", "2")
                .param("username", "root")
                .param("password", "attackerhash")
                .param("enabled", "false")
                .param("primaryAccount.accountBalance", "1000000")
                .param("firstName", "Alice")
                .param("lastName", "Smith")
                .param("email", "alice@example.com")
                .param("phone", "111"))
                .andExpect(status().isOk());

        assertEquals(Long.valueOf(1L), alice.getUserId());
        assertEquals("alice", alice.getUsername());
        assertEquals("alicehash", alice.getPassword());
        assertEquals(true, alice.isEnabled());
        verify(userService).saveUser(alice);
        verify(userService, never()).findByUsername("root");
    }

    @Test
    public void rejectsEmailOwnedByAnotherUser() throws Exception {
        mockMvc.perform(post("/user/profile").principal(alicePrincipal)
                .param("firstName", "Alice")
                .param("lastName", "Smith")
                .param("email", "bob@example.com")
                .param("phone", "111"))
                .andExpect(status().isOk())
                .andExpect(view().name("profile"))
                .andExpect(model().attribute("emailExists", true))
                .andExpect(model().attribute("user", alice));

        assertEquals("alice@example.com", alice.getEmail());
        verify(userService, never()).saveUser(Mockito.any(User.class));
    }

    @Test
    public void rejectsMissingOrBlankEmail() throws Exception {
        mockMvc.perform(post("/user/profile").principal(alicePrincipal)
                .param("firstName", "Alice")
                .param("lastName", "Smith")
                .param("phone", "111"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("emailMissing", true));

        mockMvc.perform(post("/user/profile").principal(alicePrincipal)
                .param("firstName", "Alice")
                .param("lastName", "Smith")
                .param("email", "  ")
                .param("phone", "111"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("emailMissing", true));

        assertEquals("alice@example.com", alice.getEmail());
        verify(userService, never()).saveUser(Mockito.any(User.class));
    }

    private static User user(Long id, String username, String first, String last, String email, String phone,
            String password) {
        User u = new User();
        u.setUserId(id);
        u.setUsername(username);
        u.setFirstName(first);
        u.setLastName(last);
        u.setEmail(email);
        u.setPhone(phone);
        u.setPassword(password);
        return u;
    }
}
