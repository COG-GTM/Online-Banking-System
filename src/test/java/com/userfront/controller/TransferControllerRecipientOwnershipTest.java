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
import java.util.Collections;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.view.InternalResourceViewResolver;

import com.userfront.domain.PrimaryAccount;
import com.userfront.domain.Recipient;
import com.userfront.domain.SavingsAccount;
import com.userfront.domain.User;
import com.userfront.service.TransactionService;
import com.userfront.service.UserService;

public class TransferControllerRecipientOwnershipTest {

    private static final Principal ALICE = new UsernamePasswordAuthenticationToken("alice", "n/a");

    @Mock
    private TransactionService transactionService;

    @Mock
    private UserService userService;

    @InjectMocks
    private TransferController controller;

    private MockMvc mockMvc;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        InternalResourceViewResolver viewResolver = new InternalResourceViewResolver();
        viewResolver.setPrefix("/templates/");
        viewResolver.setSuffix(".html");
        mockMvc = MockMvcBuilders.standaloneSetup(controller).setViewResolvers(viewResolver).build();

        User alice = new User();
        alice.setUsername("alice");
        alice.setPrimaryAccount(new PrimaryAccount());
        alice.setSavingsAccount(new SavingsAccount());
        when(userService.findByUsername("alice")).thenReturn(alice);
        when(transactionService.findRecipientList(any(Principal.class))).thenReturn(Collections.<Recipient>emptyList());
    }

    @Test
    public void editLooksUpRecipientByIdWithinPrincipalsList() throws Exception {
        Recipient own = recipient(8L, "Sam");
        when(transactionService.findRecipientById(eq(8L), any(Principal.class))).thenReturn(own);

        mockMvc.perform(get("/transfer/recipient/edit").param("recipientId", "8").principal(ALICE))
                .andExpect(status().isOk())
                .andExpect(view().name("recipient"))
                .andExpect(model().attribute("recipient", own));

        verify(transactionService).findRecipientById(eq(8L), eq(ALICE));
    }

    @Test
    public void editOfRecipientNotInPrincipalsListIsNotFound() throws Exception {
        when(transactionService.findRecipientById(any(Long.class), any(Principal.class))).thenReturn(null);

        mockMvc.perform(get("/transfer/recipient/edit").param("recipientId", "42").principal(ALICE))
                .andExpect(status().isNotFound());
    }

    @Test
    public void deleteIsScopedToPrincipal() throws Exception {
        mockMvc.perform(get("/transfer/recipient/delete").param("recipientId", "7").principal(ALICE))
                .andExpect(status().isOk());

        verify(transactionService).deleteRecipientById(eq(7L), eq(ALICE));
    }

    @Test
    public void saveWithForeignRecipientIdIsRejected() throws Exception {
        when(transactionService.findRecipientById(eq(42L), any(Principal.class))).thenReturn(null);

        mockMvc.perform(post("/transfer/recipient/save").param("id", "42").param("name", "Hijack").principal(ALICE))
                .andExpect(status().isNotFound());

        verify(transactionService, never()).saveRecipient(any(Recipient.class));
    }

    @Test
    public void saveWithOwnRecipientIdUpdates() throws Exception {
        when(transactionService.findRecipientById(eq(7L), any(Principal.class))).thenReturn(recipient(7L, "Bob"));

        mockMvc.perform(post("/transfer/recipient/save").param("id", "7").param("name", "Bobby").principal(ALICE))
                .andExpect(status().is3xxRedirection());

        verify(transactionService).saveRecipient(any(Recipient.class));
    }

    @Test
    public void saveOfNewRecipientSkipsOwnershipLookup() throws Exception {
        mockMvc.perform(post("/transfer/recipient/save").param("name", "Carol").principal(ALICE))
                .andExpect(status().is3xxRedirection());

        verify(transactionService, never()).findRecipientById(any(Long.class), any(Principal.class));
        verify(transactionService).saveRecipient(any(Recipient.class));
    }

    @Test
    public void transferToRecipientNotInPrincipalsListIsNotFoundAndMovesNoMoney() throws Exception {
        when(transactionService.findRecipientById(any(Long.class), any(Principal.class))).thenReturn(null);

        mockMvc.perform(post("/transfer/toSomeoneElse")
                .param("recipientId", "42").param("accountType", "Primary").param("amount", "10")
                .principal(ALICE))
                .andExpect(status().isNotFound());

        verify(transactionService, never()).toSomeoneElseTransfer(any(Recipient.class), anyString(), anyString(),
                any(PrimaryAccount.class), any(SavingsAccount.class));
    }

    @Test
    public void transferToOwnRecipientProceeds() throws Exception {
        Recipient own = recipient(7L, "Bob");
        when(transactionService.findRecipientById(eq(7L), any(Principal.class))).thenReturn(own);

        mockMvc.perform(post("/transfer/toSomeoneElse")
                .param("recipientId", "7").param("accountType", "Primary").param("amount", "10")
                .principal(ALICE))
                .andExpect(status().is3xxRedirection());

        verify(transactionService).toSomeoneElseTransfer(eq(own), eq("Primary"), eq("10"),
                any(PrimaryAccount.class), any(SavingsAccount.class));
    }

    private static Recipient recipient(Long id, String name) {
        Recipient recipient = new Recipient();
        recipient.setId(id);
        recipient.setName(name);
        return recipient;
    }
}
