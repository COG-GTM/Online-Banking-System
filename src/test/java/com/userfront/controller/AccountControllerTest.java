package com.userfront.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.security.Principal;

import org.junit.Before;
import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.ViewResolver;

import com.userfront.domain.AccountType;
import com.userfront.service.AccountService;

public class AccountControllerTest {

    private static final Principal ALICE = () -> "alice";

    private AccountService accountService;
    private MockMvc mockMvc;

    @Before
    public void setUp() {
        accountService = mock(AccountService.class);
        AccountController controller = new AccountController();
        ReflectionTestUtils.setField(controller, "accountService", accountService);
        View noOp = (model, request, response) -> { };
        ViewResolver resolver = (viewName, locale) -> noOp;
        mockMvc = MockMvcBuilders.standaloneSetup(controller).setViewResolvers(resolver).build();
    }

    @Test
    public void depositToKnownAccountTypes() throws Exception {
        mockMvc.perform(post("/account/deposit").principal(ALICE).param("accountType", "Primary").param("amount", "10"))
                .andExpect(view().name("redirect:/userFront"));
        mockMvc.perform(post("/account/deposit").principal(ALICE).param("accountType", "Savings").param("amount", "5"))
                .andExpect(view().name("redirect:/userFront"));

        verify(accountService).deposit(AccountType.PRIMARY, 10d, ALICE);
        verify(accountService).deposit(AccountType.SAVINGS, 5d, ALICE);
    }

    @Test
    public void withdrawFromKnownAccountTypes() throws Exception {
        mockMvc.perform(post("/account/withdraw").principal(ALICE).param("accountType", "Savings").param("amount", "3"))
                .andExpect(view().name("redirect:/userFront"));

        verify(accountService).withdraw(AccountType.SAVINGS, 3d, ALICE);
    }

    @Test
    public void depositRejectsUnknownAccountType() throws Exception {
        for (String accountType : new String[] {"Checking", "primary", "", "Primary,Savings"}) {
            mockMvc.perform(post("/account/deposit").principal(ALICE).param("accountType", accountType).param("amount", "10"))
                    .andExpect(status().isBadRequest())
                    .andExpect(view().name("deposit"))
                    .andExpect(model().attribute("invalidAccountType", true));
        }
        mockMvc.perform(post("/account/deposit").principal(ALICE).param("amount", "10"))
                .andExpect(status().isBadRequest())
                .andExpect(view().name("deposit"));

        verify(accountService, never()).deposit(any(), anyDouble(), any());
    }

    @Test
    public void withdrawRejectsUnknownAccountType() throws Exception {
        mockMvc.perform(post("/account/withdraw").principal(ALICE).param("accountType", "Brokerage").param("amount", "10"))
                .andExpect(status().isBadRequest())
                .andExpect(view().name("withdraw"))
                .andExpect(model().attribute("invalidAccountType", true));

        verify(accountService, never()).withdraw(any(), anyDouble(), eq(ALICE));
    }
}
