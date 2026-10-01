package com.userfront.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.security.Principal;
import java.util.Collections;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.http.HttpStatus;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.web.server.ResponseStatusException;

import com.userfront.domain.Recipient;
import com.userfront.service.TransactionService;
import com.userfront.service.UserService;

@RunWith(MockitoJUnitRunner.class)
public class TransferControllerTest {

    @Mock
    private TransactionService transactionService;

    @Mock
    private UserService userService;

    @InjectMocks
    private TransferController transferController;

    private final Principal alice = () -> "alice";

    @Test
    public void recipientEditRendersOwnRecipient() {
        Recipient recipient = new Recipient();
        when(transactionService.findRecipientByName("Bob", alice)).thenReturn(recipient);
        when(transactionService.findRecipientList(alice)).thenReturn(Collections.singletonList(recipient));
        ExtendedModelMap model = new ExtendedModelMap();

        String view = transferController.recipientEdit("Bob", model, alice);

        assertEquals("recipient", view);
        assertSame(recipient, model.get("recipient"));
    }

    @Test
    public void recipientEditReturnsNotFoundForOtherUsersRecipient() {
        when(transactionService.findRecipientByName("Mallory", alice)).thenReturn(null);
        ExtendedModelMap model = new ExtendedModelMap();

        try {
            transferController.recipientEdit("Mallory", model, alice);
            fail("expected 404");
        } catch (ResponseStatusException e) {
            assertEquals(HttpStatus.NOT_FOUND, e.getStatus());
        }
        verify(transactionService, never()).findRecipientByName("Mallory");
        assertEquals(false, model.containsAttribute("recipient"));
    }
}
