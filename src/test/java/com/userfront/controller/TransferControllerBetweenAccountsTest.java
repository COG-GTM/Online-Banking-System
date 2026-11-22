package com.userfront.controller;

import static org.mockito.ArgumentMatchers.any;
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
import com.userfront.domain.SavingsAccount;
import com.userfront.domain.User;
import com.userfront.service.InvalidTransferException;
import com.userfront.service.TransactionService;
import com.userfront.service.UserService;

@RunWith(MockitoJUnitRunner.class)
public class TransferControllerBetweenAccountsTest {

    private static final Principal ALICE = () -> "alice";

    @Mock
    private TransactionService transactionService;

    @Mock
    private UserService userService;

    @InjectMocks
    private TransferController transferController;

    private MockMvc mockMvc;
    private User user;

    @Before
    public void setUp() {
        InternalResourceViewResolver viewResolver = new InternalResourceViewResolver();
        viewResolver.setPrefix("/templates/");
        viewResolver.setSuffix(".html");
        mockMvc = MockMvcBuilders.standaloneSetup(transferController).setViewResolvers(viewResolver).build();

        user = new User();
        user.setUsername("alice");
        user.setPrimaryAccount(new PrimaryAccount());
        user.setSavingsAccount(new SavingsAccount());
        when(userService.findByUsername("alice")).thenReturn(user);
    }

    @Test
    public void validTransferRedirectsToUserFront() throws Exception {
        mockMvc.perform(post("/transfer/betweenAccounts").principal(ALICE)
                .param("transferFrom", "Primary").param("transferTo", "Savings").param("amount", "10"))
                .andExpect(status().is3xxRedirection())
                .andExpect(view().name("redirect:/userFront"));

        verify(transactionService).betweenAccountsTransfer(eq("Primary"), eq("Savings"), eq("10"),
                eq(user.getPrimaryAccount()), eq(user.getSavingsAccount()));
    }

    @Test
    public void invalidTransferRerendersFormWithError() throws Exception {
        doThrow(new InvalidTransferException("Please choose two different accounts to transfer between."))
                .when(transactionService).betweenAccountsTransfer(any(), any(), any(), any(), any());

        mockMvc.perform(post("/transfer/betweenAccounts").principal(ALICE)
                .param("transferFrom", "Primary").param("transferTo", "Primary").param("amount", "10"))
                .andExpect(status().isOk())
                .andExpect(view().name("betweenAccounts"))
                .andExpect(model().attribute("transferError", "Please choose two different accounts to transfer between."))
                .andExpect(model().attribute("amount", "10"));
    }
}
