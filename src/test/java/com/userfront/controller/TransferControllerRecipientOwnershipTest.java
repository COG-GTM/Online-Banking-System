package com.userfront.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.security.Principal;
import java.util.Collections;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;

import com.userfront.domain.PrimaryAccount;
import com.userfront.domain.Recipient;
import com.userfront.domain.SavingsAccount;
import com.userfront.domain.User;
import com.userfront.exception.RecipientNotFoundException;
import com.userfront.service.TransactionService;
import com.userfront.service.UserService;

@RunWith(MockitoJUnitRunner.Silent.class)
public class TransferControllerRecipientOwnershipTest {

    @Mock
    private TransactionService transactionService;

    @Mock
    private UserService userService;

    @InjectMocks
    private TransferController controller;

    private Principal alice;

    @Before
    public void setUp() {
        alice = mock(Principal.class);
        when(alice.getName()).thenReturn("alice");
        when(transactionService.findRecipientList(alice)).thenReturn(Collections.<Recipient>emptyList());
    }

    @Test
    public void editRendersRecipientOwnedByPrincipal() {
        Recipient owned = new Recipient();
        owned.setName("Bob");
        when(transactionService.findRecipientByName("Bob", alice)).thenReturn(owned);
        Model model = new ExtendedModelMap();

        String view = controller.recipientEdit("Bob", model, alice);

        assertEquals("recipient", view);
        assertSame(owned, model.asMap().get("recipient"));
    }

    @Test(expected = RecipientNotFoundException.class)
    public void editOfAnotherCustomersRecipientIsNotFound() {
        when(transactionService.findRecipientByName("Mallory", alice)).thenReturn(null);

        controller.recipientEdit("Mallory", new ExtendedModelMap(), alice);
    }

    @Test
    public void deleteOfAnotherCustomersRecipientIsNotFound() {
        when(transactionService.deleteRecipientByName("Mallory", alice)).thenReturn(false);

        try {
            controller.recipientDelete("Mallory", new ExtendedModelMap(), alice);
        } catch (RecipientNotFoundException expected) {
            return;
        }
        throw new AssertionError("expected RecipientNotFoundException");
    }

    @Test
    public void saveWithForeignRecipientIdIsRejected() {
        Recipient submitted = new Recipient();
        submitted.setId(42L);
        when(transactionService.findRecipientById(42L, alice)).thenReturn(null);

        try {
            controller.recipientPost(submitted, alice);
        } catch (RecipientNotFoundException expected) {
            verify(transactionService, never()).saveRecipient(any(Recipient.class));
            return;
        }
        throw new AssertionError("expected RecipientNotFoundException");
    }

    @Test
    public void saveOfNewRecipientIsAssignedToPrincipal() {
        User user = new User();
        when(userService.findByUsername("alice")).thenReturn(user);
        Recipient submitted = new Recipient();

        controller.recipientPost(submitted, alice);

        assertSame(user, submitted.getUser());
        verify(transactionService).saveRecipient(submitted);
    }

    @Test
    public void transferToAnotherCustomersRecipientIsRejected() {
        when(transactionService.findRecipientByName("Mallory", alice)).thenReturn(null);

        try {
            controller.toSomeoneElsePost("Mallory", "Primary", "10", alice);
        } catch (RecipientNotFoundException expected) {
            verify(transactionService, never()).toSomeoneElseTransfer(any(Recipient.class), anyString(), anyString(),
                    any(PrimaryAccount.class), any(SavingsAccount.class));
            return;
        }
        throw new AssertionError("expected RecipientNotFoundException");
    }
}
