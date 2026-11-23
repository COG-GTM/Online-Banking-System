package com.userfront.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.math.BigDecimal;
import java.security.Principal;

import org.junit.Before;
import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;
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

public class TransferControllerTest {

    private final Principal principal = () -> "alice";

    private TransactionService transactionService;
    private MockMvc mockMvc;
    private User user;

    @Before
    public void setUp() {
        transactionService = mock(TransactionService.class);
        UserService userService = mock(UserService.class);
        user = new User();
        user.setPrimaryAccount(new PrimaryAccount());
        user.setSavingsAccount(new SavingsAccount());
        when(userService.findByUsername("alice")).thenReturn(user);

        TransferController controller = new TransferController();
        ReflectionTestUtils.setField(controller, "transactionService", transactionService);
        ReflectionTestUtils.setField(controller, "userService", userService);
        InternalResourceViewResolver viewResolver = new InternalResourceViewResolver("/templates/", ".html");
        mockMvc = MockMvcBuilders.standaloneSetup(controller).setViewResolvers(viewResolver).build();
    }

    @Test
    public void negativeBetweenAccountsAmountReRendersFormWithoutCallingService() throws Exception {
        mockMvc.perform(post("/transfer/betweenAccounts").principal(principal)
                .param("transferFrom", "Primary").param("transferTo", "Savings").param("amount", "-1000000"))
                .andExpect(view().name("betweenAccounts"))
                .andExpect(model().attributeExists("errorMessage"));

        verify(transactionService, never()).betweenAccountsTransfer(anyString(), anyString(), any(BigDecimal.class), any(PrimaryAccount.class), any(SavingsAccount.class));
    }

    @Test
    public void nanToSomeoneElseAmountReRendersFormWithoutCallingService() throws Exception {
        mockMvc.perform(post("/transfer/toSomeoneElse").principal(principal)
                .param("recipientName", "bob").param("accountType", "Primary").param("amount", "NaN"))
                .andExpect(view().name("toSomeoneElse"))
                .andExpect(model().attributeExists("errorMessage", "recipientList"));

        verify(transactionService, never()).toSomeoneElseTransfer(any(Recipient.class), anyString(), any(BigDecimal.class), any(PrimaryAccount.class), any(SavingsAccount.class));
    }

    @Test
    public void overdraftTransferReRendersFormWithError() throws Exception {
        doThrow(new InsufficientFundsException()).when(transactionService)
                .betweenAccountsTransfer(eq("Primary"), eq("Savings"), eq(new BigDecimal("500.00")), any(PrimaryAccount.class), any(SavingsAccount.class));

        mockMvc.perform(post("/transfer/betweenAccounts").principal(principal)
                .param("transferFrom", "Primary").param("transferTo", "Savings").param("amount", "500"))
                .andExpect(view().name("betweenAccounts"))
                .andExpect(model().attribute("errorMessage", "Insufficient funds: the amount exceeds the available balance."));
    }

    @Test
    public void validTransferPassesBigDecimalAndRedirects() throws Exception {
        Recipient bob = new Recipient();
        when(transactionService.findRecipientByName("bob")).thenReturn(bob);

        mockMvc.perform(post("/transfer/toSomeoneElse").principal(principal)
                .param("recipientName", "bob").param("accountType", "Savings").param("amount", "12.5"))
                .andExpect(view().name("redirect:/userFront"));

        verify(transactionService).toSomeoneElseTransfer(eq(bob), eq("Savings"), eq(new BigDecimal("12.50")), any(PrimaryAccount.class), any(SavingsAccount.class));
    }
}
