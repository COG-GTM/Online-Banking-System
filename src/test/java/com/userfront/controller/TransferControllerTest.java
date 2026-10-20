package com.userfront.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
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
import org.springframework.web.servlet.view.InternalResourceViewResolver;

import com.userfront.domain.PrimaryAccount;
import com.userfront.domain.Recipient;
import com.userfront.domain.SavingsAccount;
import com.userfront.domain.User;
import com.userfront.service.TransactionService;
import com.userfront.service.UserService;
import com.userfront.service.transfer.InsufficientFundsException;
import com.userfront.service.transfer.InvalidTransferException;

@RunWith(MockitoJUnitRunner.class)
public class TransferControllerTest {

    @Mock
    private TransactionService transactionService;
    @Mock
    private UserService userService;

    @InjectMocks
    private TransferController controller;

    private MockMvc mockMvc;
    private final Principal alice = () -> "alice";
    private User user;

    @Before
    public void setUp() {
        InternalResourceViewResolver resolver = new InternalResourceViewResolver();
        resolver.setSuffix(".html");
        mockMvc = MockMvcBuilders.standaloneSetup(controller).setViewResolvers(resolver).build();
        user = new User();
        user.setPrimaryAccount(new PrimaryAccount());
        user.setSavingsAccount(new SavingsAccount());
        when(userService.findByUsername("alice")).thenReturn(user);
    }

    @Test
    public void toSomeoneElseResolvesRecipientWithinCallersList() throws Exception {
        Recipient bob = new Recipient();
        when(transactionService.findRecipientByName("Bob", "alice")).thenReturn(bob);

        mockMvc.perform(post("/transfer/toSomeoneElse").principal(alice)
                .param("recipientName", "Bob").param("accountType", "Primary").param("amount", "10"))
                .andExpect(view().name("redirect:/userFront"));

        verify(transactionService).toSomeoneElseTransfer(bob, "Primary", "10", user.getPrimaryAccount(), user.getSavingsAccount());
    }

    @Test
    public void toSomeoneElseNegativeAmountReRendersFormWith400() throws Exception {
        doThrow(new InvalidTransferException("Amount must be a positive number with at most 2 decimal places."))
                .when(transactionService).toSomeoneElseTransfer(any(), anyString(), eq("-1000000"), any(), any());

        mockMvc.perform(post("/transfer/toSomeoneElse").principal(alice)
                .param("recipientName", "Bob").param("accountType", "Primary").param("amount", "-1000000"))
                .andExpect(status().isBadRequest())
                .andExpect(view().name("toSomeoneElse"))
                .andExpect(model().attributeExists("errorMessage", "recipientList"));
    }

    @Test
    public void betweenAccountsOverdraftReRendersFormWith400() throws Exception {
        doThrow(new InsufficientFundsException("Insufficient funds for this transfer."))
                .when(transactionService).betweenAccountsTransfer(eq("Primary"), eq("Savings"), eq("999999"), any(), any());

        mockMvc.perform(post("/transfer/betweenAccounts").principal(alice)
                .param("transferFrom", "Primary").param("transferTo", "Savings").param("amount", "999999"))
                .andExpect(status().isBadRequest())
                .andExpect(view().name("betweenAccounts"))
                .andExpect(model().attribute("errorMessage", "Insufficient funds for this transfer."));
    }

    @Test
    public void toSomeoneElseAmbiguousPayeeReRendersFormWith400() throws Exception {
        when(transactionService.findRecipientByName("Sam", "alice"))
                .thenThrow(new InvalidTransferException("More than one payee is named \"Sam\"; rename one before transferring."));

        mockMvc.perform(post("/transfer/toSomeoneElse").principal(alice)
                .param("recipientName", "Sam").param("accountType", "Primary").param("amount", "10"))
                .andExpect(status().isBadRequest())
                .andExpect(view().name("toSomeoneElse"));
    }
}
