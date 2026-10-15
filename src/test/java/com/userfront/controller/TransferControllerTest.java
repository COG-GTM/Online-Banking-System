package com.userfront.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.userfront.domain.PrimaryAccount;
import com.userfront.domain.Recipient;
import com.userfront.domain.SavingsAccount;
import com.userfront.domain.User;
import com.userfront.service.TransactionService;
import com.userfront.service.UserService;
import com.userfront.service.exception.RecipientNotFoundException;

@RunWith(MockitoJUnitRunner.class)
public class TransferControllerTest {

    private final Principal alice = () -> "alice";

    @Mock
    private TransactionService transactionService;

    @Mock
    private UserService userService;

    @InjectMocks
    private TransferController controller;

    private MockMvc mockMvc;

    @Before
    public void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    public void editLoadsCallersOwnRecipient() throws Exception {
        Recipient own = new Recipient();
        when(transactionService.findRecipientById(7L, alice)).thenReturn(own);

        mockMvc.perform(get("/transfer/recipient/edit").param("recipientId", "7").principal(alice))
                .andExpect(status().isOk())
                .andExpect(view().name("recipient"))
                .andExpect(model().attribute("recipient", own));
    }

    @Test
    public void editOfOtherUsersRecipientIs404() throws Exception {
        when(transactionService.findRecipientById(42L, alice)).thenThrow(new RecipientNotFoundException());

        mockMvc.perform(get("/transfer/recipient/edit").param("recipientId", "42").principal(alice))
                .andExpect(status().isNotFound());
    }

    @Test
    public void transferToOtherUsersRecipientIs404AndMovesNoMoney() throws Exception {
        when(userService.findByUsername("alice")).thenReturn(new User());
        when(transactionService.findRecipientById(42L, alice)).thenThrow(new RecipientNotFoundException());

        mockMvc.perform(post("/transfer/toSomeoneElse")
                        .param("recipientId", "42").param("accountType", "Primary").param("amount", "10")
                        .principal(alice))
                .andExpect(status().isNotFound());

        verify(transactionService, never()).toSomeoneElseTransfer(any(), anyString(), anyString(), any(), any());
    }

    @Test
    public void transferToOwnRecipientUsesScopedLookup() throws Exception {
        User user = new User();
        PrimaryAccount primary = new PrimaryAccount();
        SavingsAccount savings = new SavingsAccount();
        user.setPrimaryAccount(primary);
        user.setSavingsAccount(savings);
        Recipient own = new Recipient();
        when(userService.findByUsername("alice")).thenReturn(user);
        when(transactionService.findRecipientById(7L, alice)).thenReturn(own);

        mockMvc.perform(post("/transfer/toSomeoneElse")
                        .param("recipientId", "7").param("accountType", "Primary").param("amount", "10")
                        .principal(alice))
                .andExpect(status().is3xxRedirection());

        verify(transactionService).toSomeoneElseTransfer(eq(own), eq("Primary"), eq("10"), eq(primary), eq(savings));
    }

    @Test
    public void deleteOfOtherUsersRecipientIs404() throws Exception {
        org.mockito.Mockito.doThrow(new RecipientNotFoundException())
                .when(transactionService).deleteRecipientById(42L, alice);

        mockMvc.perform(get("/transfer/recipient/delete").param("recipientId", "42").principal(alice))
                .andExpect(status().isNotFound());
    }
}
