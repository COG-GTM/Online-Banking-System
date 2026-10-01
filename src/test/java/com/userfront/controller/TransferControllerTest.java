package com.userfront.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.security.Principal;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.http.HttpStatus;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.web.server.ResponseStatusException;

import com.userfront.domain.PrimaryAccount;
import com.userfront.domain.Recipient;
import com.userfront.domain.SavingsAccount;
import com.userfront.domain.User;
import com.userfront.service.TransactionService;
import com.userfront.service.UserService;

@RunWith(MockitoJUnitRunner.Silent.class)
public class TransferControllerTest {

    @Mock
    private TransactionService transactionService;

    @Mock
    private UserService userService;

    @InjectMocks
    private TransferController controller;

    private final Principal alice = () -> "alice";

    private User aliceUser;

    @Before
    public void setUp() {
        aliceUser = new User();
        aliceUser.setUsername("alice");
        aliceUser.setPrimaryAccount(new PrimaryAccount());
        aliceUser.setSavingsAccount(new SavingsAccount());
        when(userService.findByUsername("alice")).thenReturn(aliceUser);
    }

    @Test
    public void toSomeoneElseRejectsRecipientOwnedByAnotherUser() {
        when(transactionService.findRecipientByName("bob-payee", alice)).thenReturn(null);

        assertNotFound(() -> controller.toSomeoneElsePost("bob-payee", "Primary", "100", alice));

        verify(transactionService, never()).toSomeoneElseTransfer(any(), anyString(), anyString(), any(), any());
    }

    @Test
    public void toSomeoneElseTransfersToOwnedRecipient() {
        Recipient owned = new Recipient();
        owned.setName("landlord");
        when(transactionService.findRecipientByName("landlord", alice)).thenReturn(owned);

        assertEquals("redirect:/userFront", controller.toSomeoneElsePost("landlord", "Savings", "25", alice));

        verify(transactionService).toSomeoneElseTransfer(owned, "Savings", "25",
                aliceUser.getPrimaryAccount(), aliceUser.getSavingsAccount());
    }

    @Test
    public void recipientEditRejectsRecipientOwnedByAnotherUser() {
        assertNotFound(() -> controller.recipientEdit("bob-payee", new ExtendedModelMap(), alice));
    }

    @Test
    public void recipientDeleteRejectsRecipientOwnedByAnotherUser() {
        assertNotFound(() -> controller.recipientDelete("bob-payee", new ExtendedModelMap(), alice));

        verify(transactionService, never()).deleteRecipientByName(anyString(), any());
    }

    @Test
    public void recipientDeleteRemovesOwnedRecipient() {
        when(transactionService.findRecipientByName("landlord", alice)).thenReturn(new Recipient());

        controller.recipientDelete("landlord", new ExtendedModelMap(), alice);

        verify(transactionService).deleteRecipientByName("landlord", alice);
    }

    @Test
    public void recipientSaveRejectsOverwritingAnotherUsersRecipient() {
        Recipient submitted = new Recipient();
        submitted.setId(42L);
        when(transactionService.findRecipientById(42L, alice)).thenReturn(null);

        assertNotFound(() -> controller.recipientPost(submitted, alice));

        verify(transactionService, never()).saveRecipient(any());
    }

    @Test
    public void recipientSaveAssignsNewRecipientToPrincipal() {
        Recipient submitted = new Recipient();
        submitted.setName("landlord");

        controller.recipientPost(submitted, alice);

        assertEquals(aliceUser, submitted.getUser());
        verify(transactionService).saveRecipient(submitted);
    }

    private static void assertNotFound(Runnable call) {
        try {
            call.run();
            fail("expected ResponseStatusException");
        } catch (ResponseStatusException e) {
            assertEquals(HttpStatus.NOT_FOUND, e.getStatus());
        }
    }
}
