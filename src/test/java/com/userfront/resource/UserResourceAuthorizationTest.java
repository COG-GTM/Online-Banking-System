package com.userfront.resource;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Date;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;

import com.userfront.config.SecurityConfig;
import com.userfront.domain.PrimaryTransaction;
import com.userfront.domain.SavingsTransaction;
import com.userfront.domain.User;
import com.userfront.service.TransactionService;
import com.userfront.service.UserService;
import com.userfront.service.UserServiceImpl.UserSecurityService;

@RunWith(SpringRunner.class)
@WebMvcTest(UserResource.class)
@Import(SecurityConfig.class)
public class UserResourceAuthorizationTest {

    private static final String PRIMARY = "/api/user/primary/transaction";
    private static final String SAVINGS = "/api/user/savings/transaction";

    @Autowired
    private MockMvc mvc;

    @MockBean
    private UserService userService;

    @MockBean
    private TransactionService transactionService;

    @MockBean
    private UserSecurityService userSecurityService;

    @Test
    public void anonymousCallerIsRedirectedToLogin() throws Exception {
        mvc.perform(get(PRIMARY).param("username", "bob"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/index"));
        mvc.perform(get(SAVINGS).param("username", "bob"))
                .andExpect(status().is3xxRedirection());
        verify(transactionService, never()).findPrimaryTransactionList(anyString());
        verify(transactionService, never()).findSavingsTransactionList(anyString());
    }

    @Test
    public void regularUserCannotReadAnotherUsersTransactions() throws Exception {
        mvc.perform(get(PRIMARY).param("username", "bob").with(user("alice").roles("USER")))
                .andExpect(status().isForbidden());
        mvc.perform(get(SAVINGS).param("username", "bob").with(user("alice").roles("USER")))
                .andExpect(status().isForbidden());
        verify(transactionService, never()).findPrimaryTransactionList(anyString());
        verify(transactionService, never()).findSavingsTransactionList(anyString());
    }

    @Test
    public void regularUserCannotUseAdminApiEvenForOwnUsername() throws Exception {
        mvc.perform(get(PRIMARY).param("username", "alice").with(user("alice").roles("USER")))
                .andExpect(status().isForbidden());
        mvc.perform(get(SAVINGS).param("username", "alice").with(user("alice").roles("USER")))
                .andExpect(status().isForbidden());
        verify(transactionService, never()).findPrimaryTransactionList(anyString());
        verify(transactionService, never()).findSavingsTransactionList(anyString());
    }

    @Test
    public void adminCanReadAnyUsersTransactions() throws Exception {
        given(userService.findByUsername("bob")).willReturn(new User());
        given(transactionService.findPrimaryTransactionList("bob")).willReturn(Collections.singletonList(
                new PrimaryTransaction(new Date(), "Deposit to Primary Account", "Account", "Finished", 25.0,
                        new BigDecimal("125.00"), null)));
        given(transactionService.findSavingsTransactionList("bob")).willReturn(Collections.<SavingsTransaction>emptyList());

        mvc.perform(get(PRIMARY).param("username", "bob").with(user("root").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].description").value("Deposit to Primary Account"));
        mvc.perform(get(SAVINGS).param("username", "bob").with(user("root").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    public void adminLookupOfUnknownUsernameReturnsNotFound() throws Exception {
        mvc.perform(get(PRIMARY).param("username", "ghost").with(user("root").roles("ADMIN")))
                .andExpect(status().isNotFound());
        mvc.perform(get(SAVINGS).param("username", "ghost").with(user("root").roles("ADMIN")))
                .andExpect(status().isNotFound());
        verify(transactionService, never()).findPrimaryTransactionList(anyString());
        verify(transactionService, never()).findSavingsTransactionList(anyString());
    }
}
