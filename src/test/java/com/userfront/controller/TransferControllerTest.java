package com.userfront.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.security.Principal;
import java.util.Collections;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.view.InternalResourceViewResolver;

import com.userfront.domain.PrimaryAccount;
import com.userfront.domain.Recipient;
import com.userfront.domain.SavingsAccount;
import com.userfront.domain.User;
import com.userfront.service.InsufficientFundsException;
import com.userfront.service.TransactionService;
import com.userfront.service.UserService;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class TransferControllerTest {

    @Mock
    private TransactionService transactionService;

    @Mock
    private UserService userService;

    @InjectMocks
    private TransferController transferController;

    private MockMvc mockMvc;

    private final Principal principal = () -> "mallory";

    @BeforeEach
    public void setUp() {
        User user = new User();
        user.setPrimaryAccount(new PrimaryAccount());
        user.setSavingsAccount(new SavingsAccount());
        when(userService.findByUsername("mallory")).thenReturn(user);
        mockMvc = MockMvcBuilders.standaloneSetup(transferController)
                .setViewResolvers(new InternalResourceViewResolver("/templates/", ".html"))
                .build();
    }

    @Test
    public void declinedBetweenAccountsTransferReRendersFormWithError() throws Exception {
        doThrow(new InsufficientFundsException()).when(transactionService)
                .betweenAccountsTransfer(eq("Primary"), eq("Savings"), eq("500"), any(), any());

        mockMvc.perform(post("/transfer/betweenAccounts").principal(principal)
                        .param("transferFrom", "Primary").param("transferTo", "Savings").param("amount", "500"))
                .andExpect(view().name("betweenAccounts"))
                .andExpect(model().attributeExists("errorMessageKey"));
    }

    @Test
    public void declinedTransferToSomeoneElseReRendersFormWithRecipients() throws Exception {
        Recipient bob = new Recipient();
        when(transactionService.findRecipientByName("bob")).thenReturn(bob);
        when(transactionService.findRecipientList(any())).thenReturn(Collections.singletonList(bob));
        doThrow(new InsufficientFundsException()).when(transactionService)
                .toSomeoneElseTransfer(eq(bob), eq("Savings"), eq("500"), any(), any());

        mockMvc.perform(post("/transfer/toSomeoneElse").principal(principal)
                        .param("recipientName", "bob").param("accountType", "Savings").param("amount", "500"))
                .andExpect(view().name("toSomeoneElse"))
                .andExpect(model().attributeExists("errorMessageKey", "recipientList"));
    }

    @Test
    public void successfulTransferRedirectsToDashboard() throws Exception {
        mockMvc.perform(post("/transfer/betweenAccounts").principal(principal)
                        .param("transferFrom", "Savings").param("transferTo", "Primary").param("amount", "5"))
                .andExpect(redirectedUrlPattern("/userFront*"));
    }
}
