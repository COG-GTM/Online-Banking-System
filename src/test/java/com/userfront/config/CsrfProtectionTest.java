package com.userfront.config;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Collections;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;

import com.userfront.controller.AccountController;
import com.userfront.controller.TransferController;
import com.userfront.controller.UserController;
import com.userfront.domain.PrimaryAccount;
import com.userfront.domain.Recipient;
import com.userfront.domain.SavingsAccount;
import com.userfront.domain.User;
import com.userfront.service.AccountService;
import com.userfront.service.TransactionService;
import com.userfront.service.UserService;
import com.userfront.service.UserServiceImpl.UserSecurityService;

@RunWith(SpringRunner.class)
@WebMvcTest(controllers = {TransferController.class, AccountController.class, UserController.class})
@Import(SecurityConfig.class)
public class CsrfProtectionTest {

    @Autowired
    private MockMvc mvc;

    @MockBean
    private TransactionService transactionService;

    @MockBean
    private UserService userService;

    @MockBean
    private AccountService accountService;

    @MockBean
    private UserSecurityService userSecurityService;

    @Before
    public void setUp() {
        User victim = new User();
        victim.setUsername("victim");
        victim.setPrimaryAccount(new PrimaryAccount());
        victim.setSavingsAccount(new SavingsAccount());
        given(userService.findByUsername("victim")).willReturn(victim);

        Recipient recipient = new Recipient();
        recipient.setName("alice");
        given(transactionService.findRecipientList(any())).willReturn(Collections.singletonList(recipient));
    }

    @Test
    public void transferToSomeoneElseWithoutCsrfTokenIsRejected() throws Exception {
        mvc.perform(post("/transfer/toSomeoneElse").with(user("victim"))
                .param("recipientName", "attacker").param("accountType", "Primary").param("amount", "1000"))
                .andExpect(status().isForbidden());

        verify(transactionService, never()).toSomeoneElseTransfer(any(), anyString(), anyString(), any(), any());
    }

    @Test
    public void transferToSomeoneElseWithCsrfTokenSucceeds() throws Exception {
        mvc.perform(post("/transfer/toSomeoneElse").with(user("victim")).with(csrf())
                .param("recipientName", "alice").param("accountType", "Primary").param("amount", "10"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/userFront"));
    }

    @Test
    public void withdrawWithoutCsrfTokenIsRejected() throws Exception {
        mvc.perform(post("/account/withdraw").with(user("victim"))
                .param("accountType", "Primary").param("amount", "1000"))
                .andExpect(status().isForbidden());

        verify(accountService, never()).withdraw(anyString(), anyDouble(), any());
    }

    @Test
    public void profileUpdateWithoutCsrfTokenIsRejected() throws Exception {
        mvc.perform(post("/user/profile").with(user("victim"))
                .param("email", "attacker@example.com"))
                .andExpect(status().isForbidden());

        verify(userService, never()).saveUser(any());
    }

    @Test
    public void recipientDeleteViaGetIsNotAllowed() throws Exception {
        mvc.perform(get("/transfer/recipient/delete").with(user("victim"))
                .param("recipientName", "alice"))
                .andExpect(status().isMethodNotAllowed());

        verify(transactionService, never()).deleteRecipientByName(anyString());
    }

    @Test
    public void recipientDeleteViaPostWithoutCsrfTokenIsRejected() throws Exception {
        mvc.perform(post("/transfer/recipient/delete").with(user("victim"))
                .param("recipientName", "alice"))
                .andExpect(status().isForbidden());

        verify(transactionService, never()).deleteRecipientByName(anyString());
    }

    @Test
    public void recipientDeleteViaPostWithCsrfTokenSucceeds() throws Exception {
        mvc.perform(post("/transfer/recipient/delete").with(user("victim")).with(csrf())
                .param("recipientName", "alice"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/transfer/recipient"));

        verify(transactionService).deleteRecipientByName("alice");
    }

    @Test
    public void recipientPageRendersDeleteAsCsrfProtectedPostForm() throws Exception {
        mvc.perform(get("/transfer/recipient").with(user("victim")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("action=\"/transfer/recipient/delete\" method=\"post\"")))
                .andExpect(content().string(containsString("name=\"_csrf\"")));
    }
}
