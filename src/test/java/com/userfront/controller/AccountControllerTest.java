package com.userfront.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.math.BigDecimal;
import java.security.Principal;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.view.InternalResourceViewResolver;

import com.userfront.service.AccountService;
import com.userfront.service.TransactionService;
import com.userfront.service.UserService;

public class AccountControllerTest {

    private final Principal principal = () -> "alice";

    @Mock
    private UserService userService;

    @Mock
    private AccountService accountService;

    @Mock
    private TransactionService transactionService;

    @InjectMocks
    private AccountController accountController;

    private MockMvc mockMvc;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        InternalResourceViewResolver viewResolver = new InternalResourceViewResolver("/templates/", ".html");
        mockMvc = MockMvcBuilders.standaloneSetup(accountController).setViewResolvers(viewResolver).build();
    }

    @Test
    public void depositPassesExactDecimalAmount() throws Exception {
        mockMvc.perform(post("/account/deposit").principal(principal)
                .param("amount", "0.1").param("accountType", "Primary"))
                .andExpect(view().name("redirect:/userFront"));

        verify(accountService).deposit(eq("Primary"), eq(new BigDecimal("0.10")), eq(principal));
    }

    @Test
    public void withdrawPassesExactDecimalAmount() throws Exception {
        mockMvc.perform(post("/account/withdraw").principal(principal)
                .param("amount", "25.35").param("accountType", "Savings"))
                .andExpect(view().name("redirect:/userFront"));

        verify(accountService).withdraw(eq("Savings"), eq(new BigDecimal("25.35")), eq(principal));
    }

    @Test
    public void nonNumericDepositReturnsFormErrorInsteadOf500() throws Exception {
        mockMvc.perform(post("/account/deposit").principal(principal)
                .param("amount", "abc").param("accountType", "Primary"))
                .andExpect(status().isOk())
                .andExpect(view().name("deposit"))
                .andExpect(model().attributeExists("amountError"))
                .andExpect(model().attribute("amount", "abc"));

        verify(accountService, never()).deposit(anyString(), any(BigDecimal.class), any(Principal.class));
    }

    @Test
    public void invalidWithdrawReturnsFormError() throws Exception {
        mockMvc.perform(post("/account/withdraw").principal(principal)
                .param("amount", "1E-600000000").param("accountType", "Primary"))
                .andExpect(status().isOk())
                .andExpect(view().name("withdraw"))
                .andExpect(model().attributeExists("amountError"));

        verify(accountService, never()).withdraw(anyString(), any(BigDecimal.class), any(Principal.class));
    }
}
