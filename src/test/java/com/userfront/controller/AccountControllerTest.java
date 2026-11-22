package com.userfront.controller;

import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.security.Principal;
import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.web.servlet.view.InternalResourceViewResolver;

import com.userfront.domain.PrimaryAccount;
import com.userfront.domain.PrimaryTransaction;
import com.userfront.domain.SavingsAccount;
import com.userfront.domain.SavingsTransaction;
import com.userfront.domain.User;
import com.userfront.service.AccountService;
import com.userfront.service.UserService;

public class AccountControllerTest {

    @Mock
    private UserService userService;

    @Mock
    private AccountService accountService;

    @InjectMocks
    private AccountController controller;

    private MockMvc mockMvc;
    private User alice;
    private User bob;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        InternalResourceViewResolver viewResolver = new InternalResourceViewResolver();
        viewResolver.setSuffix(".html");
        mockMvc = MockMvcBuilders.standaloneSetup(controller).setViewResolvers(viewResolver).build();

        alice = user("alice", 1L);
        bob = user("bob", 2L);
        when(userService.findByUsername("alice")).thenReturn(alice);
        when(userService.findByUsername("bob")).thenReturn(bob);
    }

    @Test
    public void primaryAccountIsResolvedFromPrincipalAndIgnoresRequestParameters() throws Exception {
        MvcResult result = mockMvc.perform(get("/account/primaryAccount")
                .principal(principal("alice"))
                .param("username", "bob")
                .param("id", "2")
                .param("accountNumber", String.valueOf(bob.getPrimaryAccount().getAccountNumber())))
                .andExpect(status().isOk())
                .andExpect(view().name("primaryAccount"))
                .andReturn();

        assertSame(alice.getPrimaryAccount(), result.getModelAndView().getModel().get("primaryAccount"));
        assertSame(alice.getPrimaryAccount().getPrimaryTransactionList(),
                result.getModelAndView().getModel().get("primaryTransactionList"));
        verify(userService, never()).findByUsername("bob");
    }

    @Test
    public void savingsAccountIsResolvedFromPrincipalAndIgnoresRequestParameters() throws Exception {
        MvcResult result = mockMvc.perform(get("/account/savingsAccount")
                .principal(principal("alice"))
                .param("username", "bob")
                .param("id", "2")
                .param("accountNumber", String.valueOf(bob.getSavingsAccount().getAccountNumber())))
                .andExpect(status().isOk())
                .andExpect(view().name("savingsAccount"))
                .andReturn();

        assertSame(alice.getSavingsAccount(), result.getModelAndView().getModel().get("savingsAccount"));
        assertSame(alice.getSavingsAccount().getSavingsTransactionList(),
                result.getModelAndView().getModel().get("savingsTransactionList"));
        verify(userService, never()).findByUsername("bob");
    }

    @Test
    public void accountViewsOnlyAcceptGet() throws Exception {
        mockMvc.perform(post("/account/primaryAccount").principal(principal("alice")))
                .andExpect(status().isMethodNotAllowed());
        mockMvc.perform(post("/account/savingsAccount").principal(principal("alice")))
                .andExpect(status().isMethodNotAllowed());
    }

    @Test(expected = AccessDeniedException.class)
    public void primaryAccountRejectsMissingPrincipal() {
        controller.primaryAccount(new ExtendedModelMap(), null);
    }

    @Test(expected = AccessDeniedException.class)
    public void savingsAccountRejectsUnknownPrincipal() {
        controller.savingsAccount(new ExtendedModelMap(), principal("mallory"));
    }

    private static Principal principal(final String name) {
        Principal principal = Mockito.mock(Principal.class);
        when(principal.getName()).thenReturn(name);
        return principal;
    }

    private static User user(String username, Long id) {
        User user = new User();
        user.setUserId(id);
        user.setUsername(username);

        PrimaryAccount primaryAccount = new PrimaryAccount();
        primaryAccount.setId(id);
        primaryAccount.setAccountNumber(11220000 + id.intValue());
        List<PrimaryTransaction> primaryTransactions = new ArrayList<PrimaryTransaction>();
        primaryTransactions.add(new PrimaryTransaction());
        primaryAccount.setPrimaryTransactionList(primaryTransactions);
        user.setPrimaryAccount(primaryAccount);

        SavingsAccount savingsAccount = new SavingsAccount();
        savingsAccount.setId(id);
        savingsAccount.setAccountNumber(11230000 + id.intValue());
        List<SavingsTransaction> savingsTransactions = new ArrayList<SavingsTransaction>();
        savingsTransactions.add(new SavingsTransaction());
        savingsAccount.setSavingsTransactionList(savingsTransactions);
        user.setSavingsAccount(savingsAccount);

        return user;
    }
}
