package com.userfront.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
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
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;

import com.userfront.domain.PrimaryAccount;
import com.userfront.domain.SavingsAccount;
import com.userfront.domain.User;
import com.userfront.service.EmailAlreadyExistsException;
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

    private User storedAttacker() {
        User current = new User();
        current.setUsername("attacker");
        current.setFirstName("Old");
        current.setPrimaryAccount(new PrimaryAccount());
        current.setSavingsAccount(new SavingsAccount());
        return current;
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
        assertNull(model.asMap().get("emailExists"));
    }

    @Test
    public void profilePostRejectsEmailOwnedByAnotherUserAndKeepsSubmittedEdits() {
        assertEmailRejected(new EmailAlreadyExistsException("victim@example.com"));
    }

    @Test
    public void profilePostReportsDuplicateEmailWhenUniqueConstraintFails() {
        User victim = new User();
        victim.setUsername("victim");
        when(userService.findByEmail("victim@example.com")).thenReturn(victim);
        assertEmailRejected(new DataIntegrityViolationException("duplicate email"));
    }

    @Test(expected = DataIntegrityViolationException.class)
    public void profilePostDoesNotReportOtherIntegrityErrorsAsDuplicateEmail() {
        User form = profileForm("attacker", null);
        when(userService.updateProfile("attacker", form))
                .thenThrow(new DataIntegrityViolationException("email cannot be null"));

        userController.profilePost(form, attacker, new ExtendedModelMap());
    }

    private void assertEmailRejected(RuntimeException failure) {
        User form = profileForm("victim", "victim@example.com");
        User current = storedAttacker();
        when(userService.updateProfile("attacker", form)).thenThrow(failure);
        when(userService.findByUsername("attacker")).thenReturn(current);

        Model model = new ExtendedModelMap();
        String view = userController.profilePost(form, attacker, model);

        assertEquals("profile", view);
        assertEquals(Boolean.TRUE, model.asMap().get("emailExists"));
        User shown = (User) model.asMap().get("user");
        assertEquals("attacker", shown.getUsername());
        assertEquals("Evil", shown.getFirstName());
        assertEquals("555-0000", shown.getPhone());
        assertEquals(current.getPrimaryAccount(), shown.getPrimaryAccount());
        assertEquals(current.getSavingsAccount(), shown.getSavingsAccount());
        verify(userService, never()).findByUsername("victim");
        verify(userService, never()).saveUser(any(User.class));
    }
}
