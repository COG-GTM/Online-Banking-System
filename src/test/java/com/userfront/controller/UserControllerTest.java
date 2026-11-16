package com.userfront.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.security.Principal;

import org.junit.Before;
import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.ui.ExtendedModelMap;

import com.userfront.domain.User;
import com.userfront.service.UserService;

public class UserControllerTest {

    private UserService userService;
    private UserController controller;
    private User alice;
    private User mallory;
    private Principal malloryPrincipal;

    @Before
    public void setUp() {
        userService = mock(UserService.class);
        controller = new UserController();
        ReflectionTestUtils.setField(controller, "userService", userService);

        alice = user(1L, "alice", "alice@example.com");
        mallory = user(2L, "mallory", "mallory@example.com");
        when(userService.findByUsername("alice")).thenReturn(alice);
        when(userService.findByUsername("mallory")).thenReturn(mallory);
        when(userService.findByEmail("alice@example.com")).thenReturn(alice);
        when(userService.findByEmail("mallory@example.com")).thenReturn(mallory);
        malloryPrincipal = () -> "mallory";
    }

    @Test
    public void updatesOnlyTheLoggedInUser() {
        ProfileForm form = form("evil@example.com");
        ExtendedModelMap model = new ExtendedModelMap();

        controller.profilePost(form, malloryPrincipal, model);

        assertEquals("evil@example.com", mallory.getEmail());
        assertEquals("New", mallory.getFirstName());
        assertEquals("mallory", mallory.getUsername());
        assertEquals("alice@example.com", alice.getEmail());
        assertSame(mallory, model.get("user"));
        verify(userService).saveUser(mallory);
        verify(userService, never()).saveUser(alice);
    }

    @Test
    public void rejectsEmailOwnedByAnotherUser() {
        ExtendedModelMap model = new ExtendedModelMap();

        controller.profilePost(form("alice@example.com"), malloryPrincipal, model);

        assertTrue((Boolean) model.get("emailExists"));
        assertEquals("mallory@example.com", mallory.getEmail());
        assertSame(mallory, model.get("user"));
        verify(userService, never()).saveUser(any());
    }

    @Test
    public void allowsKeepingOwnEmail() {
        ExtendedModelMap model = new ExtendedModelMap();

        controller.profilePost(form("mallory@example.com"), malloryPrincipal, model);

        assertEquals(null, model.get("emailExists"));
        verify(userService).saveUser(mallory);
    }

    private static ProfileForm form(String email) {
        ProfileForm form = new ProfileForm();
        form.setFirstName("New");
        form.setLastName("Name");
        form.setEmail(email);
        form.setPhone("555-0100");
        return form;
    }

    private static User user(Long id, String username, String email) {
        User user = new User();
        user.setUserId(id);
        user.setUsername(username);
        user.setEmail(email);
        return user;
    }
}
