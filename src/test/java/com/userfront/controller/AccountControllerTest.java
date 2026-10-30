package com.userfront.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
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

import com.userfront.service.AccountService;
import com.userfront.service.InsufficientFundsException;

public class AccountControllerTest {

    private final Principal principal = () -> "alice";

    private AccountService accountService;
    private MockMvc mockMvc;

    @Before
    public void setUp() {
        accountService = mock(AccountService.class);
        AccountController controller = new AccountController();
        ReflectionTestUtils.setField(controller, "accountService", accountService);
        InternalResourceViewResolver viewResolver = new InternalResourceViewResolver("/templates/", ".html");
        mockMvc = MockMvcBuilders.standaloneSetup(controller).setViewResolvers(viewResolver).build();
    }

    @Test
    public void negativeWithdrawReRendersFormWithoutCallingService() throws Exception {
        mockMvc.perform(post("/account/withdraw").principal(principal)
                .param("amount", "-1000000").param("accountType", "Primary"))
                .andExpect(view().name("withdraw"))
                .andExpect(model().attributeExists("errorMessage"));

        verify(accountService, never()).withdraw(anyString(), any(BigDecimal.class), any(Principal.class));
    }

    @Test
    public void malformedDepositReRendersFormWithoutCallingService() throws Exception {
        mockMvc.perform(post("/account/deposit").principal(principal)
                .param("amount", "1E100000000").param("accountType", "Savings"))
                .andExpect(view().name("deposit"))
                .andExpect(model().attributeExists("errorMessage"));

        verify(accountService, never()).deposit(anyString(), any(BigDecimal.class), any(Principal.class));
    }

    @Test
    public void overdraftReRendersWithdrawFormWithError() throws Exception {
        doThrow(new InsufficientFundsException()).when(accountService)
                .withdraw(eq("Primary"), eq(new BigDecimal("500.00")), any(Principal.class));

        mockMvc.perform(post("/account/withdraw").principal(principal)
                .param("amount", "500").param("accountType", "Primary"))
                .andExpect(view().name("withdraw"))
                .andExpect(model().attribute("errorMessage", "Insufficient funds: the amount exceeds the available balance."));
    }

    @Test
    public void validDepositPassesBigDecimalAndRedirects() throws Exception {
        mockMvc.perform(post("/account/deposit").principal(principal)
                .param("amount", "12.5").param("accountType", "Primary"))
                .andExpect(view().name("redirect:/userFront"));

        verify(accountService).deposit(eq("Primary"), eq(new BigDecimal("12.50")), any(Principal.class));
    }
}
