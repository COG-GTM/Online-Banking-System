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

    private static final Principal ALICE = () -> "alice";

    @Mock
    private AccountService accountService;

    @Mock
    private UserService userService;

    @Mock
    private TransactionService transactionService;

    @InjectMocks
    private AccountController controller;

    private MockMvc mvc;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        InternalResourceViewResolver resolver = new InternalResourceViewResolver("/templates/", ".html");
        mvc = MockMvcBuilders.standaloneSetup(controller).setViewResolvers(resolver).build();
    }

    @Test
    public void depositsParsedBigDecimalAmount() throws Exception {
        mvc.perform(post("/account/deposit").principal(ALICE).param("accountType", "Primary").param("amount", "125.5"))
                .andExpect(status().is3xxRedirection())
                .andExpect(view().name("redirect:/userFront"));

        verify(accountService).deposit(eq("Primary"), eq(new BigDecimal("125.50")), eq(ALICE));
    }

    @Test
    public void rejectsNegativeDeposit() throws Exception {
        assertDepositRejected("Primary", "-500");
    }

    @Test
    public void rejectsZeroDeposit() throws Exception {
        assertDepositRejected("Savings", "0");
    }

    @Test
    public void rejectsNonNumericAndSpecialDeposits() throws Exception {
        for (String amount : new String[] {"", "abc", "NaN", "Infinity", "1e9", "10.001", "1000000.01"}) {
            assertDepositRejected("Primary", amount);
        }
    }

    @Test
    public void rejectsMissingAmountParameter() throws Exception {
        mvc.perform(post("/account/deposit").principal(ALICE).param("accountType", "Primary"))
                .andExpect(status().isBadRequest())
                .andExpect(view().name("deposit"));
        verify(accountService, never()).deposit(anyString(), any(BigDecimal.class), any(Principal.class));
    }

    @Test
    public void rejectsUnknownAccountType() throws Exception {
        assertDepositRejected("Checking", "10");
    }

    @Test
    public void withdrawAppliesSameValidation() throws Exception {
        mvc.perform(post("/account/withdraw").principal(ALICE).param("accountType", "Savings").param("amount", "-20"))
                .andExpect(status().isBadRequest())
                .andExpect(view().name("withdraw"))
                .andExpect(model().attributeExists("errorMessage"));
        verify(accountService, never()).withdraw(anyString(), any(BigDecimal.class), any(Principal.class));

        mvc.perform(post("/account/withdraw").principal(ALICE).param("accountType", "Savings").param("amount", "20"))
                .andExpect(view().name("redirect:/userFront"));
        verify(accountService).withdraw(eq("Savings"), eq(new BigDecimal("20.00")), eq(ALICE));
    }

    private void assertDepositRejected(String accountType, String amount) throws Exception {
        mvc.perform(post("/account/deposit").principal(ALICE).param("accountType", accountType).param("amount", amount))
                .andExpect(status().isBadRequest())
                .andExpect(view().name("deposit"))
                .andExpect(model().attributeExists("errorMessage"))
                .andExpect(model().attribute("amount", amount));
        verify(accountService, never()).deposit(anyString(), any(BigDecimal.class), any(Principal.class));
    }
}
