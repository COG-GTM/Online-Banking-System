package com.userfront.controller;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.security.Principal;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;

import com.userfront.domain.User;
import com.userfront.service.UserService;

@RunWith(MockitoJUnitRunner.class)
public class UserControllerTest {

    @Mock
    private UserService userService;

    @InjectMocks
    private UserController userController;

    private final Principal attacker = () -> "attacker";

    private User profileForm(String username, String email) {
        User form = new User();
        form.setUsername(username);
        form.setFirstName("Evil");
        form.setLastName("Hacker");
        form.setEmail(email);
        form.setPhone("555-0000");
        return form;
    }

    @Test
    public void profilePostUpdatesAuthenticatedUserIgnoringSubmittedUsername() {
        User form = profileForm("victim", "attacker@example.com");
        User updated = new User();
        updated.setUsername("attacker");
        when(userService.updateProfile("attacker", form)).thenReturn(updated);

        Model model = new ExtendedModelMap();
        String view = userController.profilePost(form, attacker, model);

        assertEquals("profile", view);
        verify(userService).updateProfile("attacker", form);
        verify(userService, never()).updateProfile(eq("victim"), any(User.class));
        verify(userService, never()).findByUsername("victim");
        verify(userService, never()).saveUser(any(User.class));
        assertEquals(updated, model.asMap().get("user"));
    }

    @Test
    public void profilePostRejectsEmailOwnedByAnotherUser() {
        User form = profileForm("attacker", "victim@example.com");
        User current = new User();
        current.setUsername("attacker");
        when(userService.isEmailTakenByAnotherUser("victim@example.com", "attacker")).thenReturn(true);
        when(userService.findByUsername("attacker")).thenReturn(current);

        Model model = new ExtendedModelMap();
        String view = userController.profilePost(form, attacker, model);

        assertEquals("profile", view);
        assertEquals(Boolean.TRUE, model.asMap().get("emailExists"));
        assertEquals(current, model.asMap().get("user"));
        verify(userService, never()).updateProfile(anyString(), any(User.class));
    }
}
