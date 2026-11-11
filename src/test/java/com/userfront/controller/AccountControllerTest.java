package com.userfront.controller;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertTrue;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.anonymous;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;

import com.userfront.domain.PrimaryAccount;
import com.userfront.domain.PrimaryTransaction;
import com.userfront.domain.SavingsAccount;
import com.userfront.domain.SavingsTransaction;
import com.userfront.domain.User;
import com.userfront.service.AccountService;
import com.userfront.service.TransactionService;
import com.userfront.service.UserService;
import com.userfront.service.UserServiceImpl.UserSecurityService;

@RunWith(SpringRunner.class)
@WebMvcTest(AccountController.class)
@WithMockUser(username = "jane")
public class AccountControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @MockBean
    private AccountService accountService;

    @MockBean
    private TransactionService transactionService;

    @MockBean
    private UserSecurityService userSecurityService;

    private static Date at(int hour, int minute) {
        Calendar calendar = Calendar.getInstance();
        calendar.clear();
        calendar.set(2026, Calendar.NOVEMBER, 11, hour, minute, 0);
        return calendar.getTime();
    }

    @Before
    public void setUp() {
        PrimaryAccount primaryAccount = new PrimaryAccount();
        primaryAccount.setAccountBalance(new BigDecimal("1169.75"));
        SavingsAccount savingsAccount = new SavingsAccount();
        savingsAccount.setAccountBalance(new BigDecimal("1500.00"));
        User user = new User();
        user.setUsername("jane");
        user.setPrimaryAccount(primaryAccount);
        user.setSavingsAccount(savingsAccount);
        given(userService.findByUsername("jane")).willReturn(user);

        PrimaryTransaction deposit = new PrimaryTransaction(at(9, 0), "Deposit to Primary Account", "Account",
                "Finished", 2500, new BigDecimal("2500"), primaryAccount);
        deposit.setId(1L);
        PrimaryTransaction payment = new PrimaryTransaction(at(10, 30), "Transfer to recipient John Smith", "Transfer",
                "Finished", 1330.25, new BigDecimal("1169.75"), primaryAccount);
        payment.setId(2L);
        given(transactionService.findPrimaryTransactionList("jane")).willReturn(Arrays.asList(deposit, payment));

        SavingsTransaction incoming = new SavingsTransaction(at(11, 0), "Between account transfer from Primary to Savings",
                "Account", "Finished", 1500, new BigDecimal("1500"), savingsAccount);
        incoming.setId(3L);
        given(transactionService.findSavingsTransactionList("jane")).willReturn(Arrays.asList(incoming));
    }

    @Test
    public void primaryAccountRendersReadableNewestFirstTransactionLog() throws Exception {
        String html = mockMvc.perform(get("/account/primaryAccount"))
                .andExpect(status().isOk())
                .andExpect(view().name("primaryAccount"))
                .andExpect(content().string(containsString("id=\"primaryTransactionTable\"")))
                .andExpect(content().string(containsString("Primary transactions")))
                .andExpect(content().string(containsString("Nov 11, 2026 10:30 AM")))
                .andExpect(content().string(containsString("+$2,500.00")))
                .andExpect(content().string(containsString("\u2212$1,330.25")))
                .andExpect(content().string(containsString("$1,169.75")))
                .andExpect(content().string(containsString(">Payment<")))
                .andExpect(content().string(containsString("data-direction=\"debit\"")))
                .andExpect(content().string(containsString("href=\"/account/primaryAccount/transactions.csv\"")))
                .andExpect(content().string(not(containsString("2500.0<"))))
                .andReturn().getResponse().getContentAsString();

        assertTrue("newest transaction should be listed first",
                html.indexOf("Transfer to recipient John Smith") < html.indexOf("Deposit to Primary Account"));
    }

    @Test
    public void savingsAccountRendersTheSameSearchableLog() throws Exception {
        mockMvc.perform(get("/account/savingsAccount"))
                .andExpect(status().isOk())
                .andExpect(view().name("savingsAccount"))
                .andExpect(content().string(containsString("id=\"savingsTransactionTable\"")))
                .andExpect(content().string(containsString("transaction-log-direction")))
                .andExpect(content().string(containsString(">Transfer in<")))
                .andExpect(content().string(containsString("+$1,500.00")))
                .andExpect(content().string(containsString("href=\"/account/savingsAccount/transactions.csv\"")));
    }

    @Test
    public void savingsAccountWithNoTransactionsStillRenders() throws Exception {
        given(transactionService.findSavingsTransactionList("jane")).willReturn(new ArrayList<>());

        mockMvc.perform(get("/account/savingsAccount"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("id=\"savingsTransactionTable\"")));
    }

    @Test
    public void primaryTransactionsDownloadAsCsv() throws Exception {
        mockMvc.perform(get("/account/primaryAccount/transactions.csv"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"primary-transactions.csv\""))
                .andExpect(content().contentTypeCompatibleWith("text/csv"))
                .andExpect(content().string(containsString(
                        "2026-11-11 10:30:00,Transfer to recipient John Smith,Payment,Transfer,Finished,-1330.25,1169.75")));
    }

    @Test
    public void savingsTransactionsDownloadAsCsv() throws Exception {
        mockMvc.perform(get("/account/savingsAccount/transactions.csv"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"savings-transactions.csv\""))
                .andExpect(content().string(containsString("Transfer in,Account,Finished,1500.00,1500.00")));
    }

    @Test
    public void transactionLogRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/account/primaryAccount/transactions.csv").with(anonymous()))
                .andExpect(status().is3xxRedirection());
    }
}
