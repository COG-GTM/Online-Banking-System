package com.userfront.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.security.Principal;
import java.util.Collections;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.view.InternalResourceViewResolver;

import com.userfront.domain.PrimaryAccount;
import com.userfront.domain.Recipient;
import com.userfront.domain.SavingsAccount;
import com.userfront.domain.User;
import com.userfront.service.TransactionService;
import com.userfront.service.UserService;

public class TransferControllerTest {

    @Mock
    private TransactionService transactionService;

    @Mock
    private UserService userService;

    @InjectMocks
    private TransferController controller;

    private MockMvc mockMvc;
    private final Principal alice = () -> "alice";
    private Recipient alicesRecipient;
    private User aliceUser;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setViewResolvers(new InternalResourceViewResolver("/templates/", ".html"))
                .build();

        alicesRecipient = new Recipient();
        alicesRecipient.setId(7L);
        alicesRecipient.setName("Carol");
        aliceUser = new User();
        aliceUser.setUsername("alice");
        aliceUser.setPrimaryAccount(new PrimaryAccount());
        aliceUser.setSavingsAccount(new SavingsAccount());

        when(transactionService.findRecipientList(any(Principal.class))).thenReturn(Collections.<Recipient>emptyList());
        when(transactionService.findRecipientById(eq(7L), any(Principal.class))).thenReturn(alicesRecipient);
        when(userService.findByUsername("alice")).thenReturn(aliceUser);
    }

    @Test
    public void transfersToOwnRecipientWithValidAmount() throws Exception {
        mockMvc.perform(post("/transfer/toSomeoneElse").principal(alice)
                .param("recipientId", "7").param("accountType", "Primary").param("amount", "25.50"))
                .andExpect(status().is3xxRedirection())
                .andExpect(view().name("redirect:/userFront"));

        verify(transactionService).findRecipientById(eq(7L), eq(alice));
        verify(transactionService).toSomeoneElseTransfer(alicesRecipient, "Primary", "25.50",
                aliceUser.getPrimaryAccount(), aliceUser.getSavingsAccount());
    }

    @Test
    public void rejectsRecipientNotOwnedByPrincipal() throws Exception {
        mockMvc.perform(post("/transfer/toSomeoneElse").principal(alice)
                .param("recipientId", "8").param("accountType", "Primary").param("amount", "10"))
                .andExpect(status().isBadRequest())
                .andExpect(view().name("toSomeoneElse"))
                .andExpect(model().attributeExists("errorMessage"));

        verify(transactionService, never()).findRecipientByName(anyString());
        verifyNoTransfer();
    }

    @Test
    public void rejectsMissingOrNonNumericRecipientId() throws Exception {
        mockMvc.perform(post("/transfer/toSomeoneElse").principal(alice)
                .param("recipientId", "Carol").param("accountType", "Primary").param("amount", "10"))
                .andExpect(status().isBadRequest())
                .andExpect(model().attributeExists("errorMessage"));
        mockMvc.perform(post("/transfer/toSomeoneElse").principal(alice)
                .param("accountType", "Primary").param("amount", "10"))
                .andExpect(status().isBadRequest())
                .andExpect(model().attributeExists("errorMessage"));

        verifyNoTransfer();
    }

    @Test
    public void rejectsNegativeAmount() throws Exception {
        assertRejectedAmount("-500");
    }

    @Test
    public void rejectsZeroAmount() throws Exception {
        assertRejectedAmount("0");
    }

    @Test
    public void rejectsNonNumericAmount() throws Exception {
        assertRejectedAmount("ten");
    }

    @Test
    public void rejectsExponentAmount() throws Exception {
        assertRejectedAmount("1E100000000");
    }

    @Test
    public void rejectsUnknownAccountType() throws Exception {
        mockMvc.perform(post("/transfer/toSomeoneElse").principal(alice)
                .param("recipientId", "7").param("accountType", "Checking").param("amount", "10"))
                .andExpect(status().isBadRequest())
                .andExpect(model().attributeExists("errorMessage"));

        verifyNoTransfer();
    }

    private void assertRejectedAmount(String amount) throws Exception {
        mockMvc.perform(post("/transfer/toSomeoneElse").principal(alice)
                .param("recipientId", "7").param("accountType", "Primary").param("amount", amount))
                .andExpect(status().isBadRequest())
                .andExpect(view().name("toSomeoneElse"))
                .andExpect(model().attributeExists("errorMessage"));

        verifyNoTransfer();
    }

    private void verifyNoTransfer() {
        verify(transactionService, never()).toSomeoneElseTransfer(any(Recipient.class), anyString(), anyString(),
                any(PrimaryAccount.class), any(SavingsAccount.class));
    }
}
