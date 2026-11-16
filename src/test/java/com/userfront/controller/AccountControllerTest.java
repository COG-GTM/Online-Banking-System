package com.userfront.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.math.BigDecimal;
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

import com.userfront.service.AccountService;
import com.userfront.service.InsufficientFundsException;
import com.userfront.service.TransactionService;
import com.userfront.service.UserService;

@RunWith(MockitoJUnitRunner.class)
public class AccountControllerTest {

    @Mock
    private AccountService accountService;

    @Mock
    private UserService userService;

    @Mock
    private TransactionService transactionService;

    @InjectMocks
    private AccountController accountController;

    private MockMvc mockMvc;

    private final Principal principal = () -> "mallory";

    @Before
    public void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(accountController)
                .setViewResolvers(new InternalResourceViewResolver("/templates/", ".html"))
                .build();
    }

    @Test
    public void negativeWithdrawReRendersFormWithoutCallingService() throws Exception {
        mockMvc.perform(post("/account/withdraw").principal(principal)
                        .param("accountType", "Primary").param("amount", "-1000000"))
                .andExpect(view().name("withdraw"))
                .andExpect(model().attributeExists("errorMessageKey"));

        verifyNoMoreInteractions(accountService);
    }

    @Test
    public void threeDecimalDepositReRendersFormWithoutCallingService() throws Exception {
        mockMvc.perform(post("/account/deposit").principal(principal)
                        .param("accountType", "Savings").param("amount", "10.005"))
                .andExpect(view().name("deposit"))
                .andExpect(model().attributeExists("errorMessageKey"));

        verifyNoMoreInteractions(accountService);
    }

    @Test
    public void nonNumericAmountReRendersFormInsteadOfFailing() throws Exception {
        mockMvc.perform(post("/account/withdraw").principal(principal)
                        .param("accountType", "Primary").param("amount", "NaN"))
                .andExpect(view().name("withdraw"))
                .andExpect(model().attributeExists("errorMessageKey"));

        verifyNoMoreInteractions(accountService);
    }

    @Test
    public void insufficientFundsReRendersWithdrawFormWithMessage() throws Exception {
        doThrow(new InsufficientFundsException()).when(accountService)
                .withdraw(eq("Primary"), eq(new BigDecimal("1000000.00")), any());

        mockMvc.perform(post("/account/withdraw").principal(principal)
                        .param("accountType", "Primary").param("amount", "1000000"))
                .andExpect(view().name("withdraw"))
                .andExpect(model().attribute("errorMessageKey", "transaction.error.insufficientFunds"));
    }

    @Test
    public void validWithdrawPassesExactBigDecimalAndRedirects() throws Exception {
        mockMvc.perform(post("/account/withdraw").principal(principal)
                        .param("accountType", "Savings").param("amount", "12.30"))
                .andExpect(view().name("redirect:/userFront"));

        verify(accountService).withdraw(eq("Savings"), eq(new BigDecimal("12.30")), any());
    }
}
