package com.userfront.controller;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;

import com.userfront.domain.PrimaryAccount;
import com.userfront.domain.SavingsAccount;
import com.userfront.domain.User;
import com.userfront.service.UserService;
import com.userfront.service.UserServiceImpl.UserSecurityService;

@RunWith(SpringRunner.class)
@WebMvcTest(UserController.class)
@AutoConfigureMockMvc(addFilters = false)
public class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @MockBean
    private UserSecurityService userSecurityService;

    private User alice;
    private User bob;

    @Before
    public void setUp() {
        alice = user(1L, "alice", "alice@example.com");
        bob = user(2L, "bob", "bob@example.com");
        given(userService.findByUsername("alice")).willReturn(alice);
        given(userService.findByUsername("bob")).willReturn(bob);
        given(userService.checkUsernameExists("bob")).willReturn(true);
        given(userService.checkEmailExists("bob@example.com")).willReturn(true);
        given(userService.saveUser(any(User.class))).willAnswer(invocation -> invocation.getArgument(0));
    }

    @After
    public void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    public void profilePostUpdatesPrincipalNotSubmittedUsername() throws Exception {
        mockMvc.perform(post("/user/profile").principal(authFor(alice))
                .param("username", "bob")
                .param("firstName", "Mallory")
                .param("lastName", "Attacker")
                .param("email", "mallory@example.com")
                .param("phone", "000"))
                .andExpect(status().isOk())
                .andExpect(view().name("profile"))
                .andExpect(model().attribute("usernameExists", true))
                .andExpect(model().attribute("user", alice));

        verify(userService, never()).saveUser(any(User.class));
        assertEquals("Bob", bob.getFirstName());
        assertEquals("bob@example.com", bob.getEmail());
        assertEquals("Alice", alice.getFirstName());
    }

    @Test
    public void profilePostIgnoresNonProfileFields() throws Exception {
        mockMvc.perform(post("/user/profile").principal(authFor(alice))
                .param("userId", "2")
                .param("password", "hacked")
                .param("enabled", "false")
                .param("username", "alice")
                .param("firstName", "Alicia")
                .param("lastName", "Smith")
                .param("email", "alicia@example.com")
                .param("phone", "555-0100"))
                .andExpect(status().isOk())
                .andExpect(view().name("profile"));

        verify(userService).saveUser(alice);
        assertEquals(Long.valueOf(1L), alice.getUserId());
        assertEquals("secret", alice.getPassword());
        assertEquals(true, alice.isEnabled());
        assertEquals("Alicia", alice.getFirstName());
        assertEquals("alicia@example.com", alice.getEmail());
        assertEquals("555-0100", alice.getPhone());
        assertEquals("Bob", bob.getFirstName());
    }

    @Test
    public void profilePostRejectsEmailOwnedByAnotherUser() throws Exception {
        mockMvc.perform(post("/user/profile").principal(authFor(alice))
                .param("username", "alice")
                .param("firstName", "Alice")
                .param("lastName", "Smith")
                .param("email", "bob@example.com")
                .param("phone", "555-0100"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("emailExists", true));

        verify(userService, never()).saveUser(any(User.class));
        assertEquals("alice@example.com", alice.getEmail());
    }

    @Test
    public void profilePostRefreshesAuthenticationOnUsernameChange() throws Exception {
        UsernamePasswordAuthenticationToken auth = authFor(alice);
        SecurityContextHolder.getContext().setAuthentication(auth);

        mockMvc.perform(post("/user/profile").principal(auth)
                .param("username", "alice2")
                .param("firstName", "Alice")
                .param("lastName", "Smith")
                .param("email", "alice@example.com")
                .param("phone", "555-0100"))
                .andExpect(status().isOk());

        verify(userService, never()).checkEmailExists(anyString());
        assertEquals("alice2", alice.getUsername());
        assertEquals("alice2", SecurityContextHolder.getContext().getAuthentication().getName());
    }

    private static UsernamePasswordAuthenticationToken authFor(User user) {
        return new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
    }

    private static User user(Long id, String username, String email) {
        User user = new User();
        user.setUserId(id);
        user.setUsername(username);
        user.setPassword("secret");
        user.setFirstName(Character.toUpperCase(username.charAt(0)) + username.substring(1));
        user.setLastName("Smith");
        user.setEmail(email);
        user.setPhone("555-0000");
        PrimaryAccount primaryAccount = new PrimaryAccount();
        primaryAccount.setAccountNumber((int) (id * 10));
        user.setPrimaryAccount(primaryAccount);
        SavingsAccount savingsAccount = new SavingsAccount();
        savingsAccount.setAccountNumber((int) (id * 10 + 1));
        user.setSavingsAccount(savingsAccount);
        return user;
    }
}
