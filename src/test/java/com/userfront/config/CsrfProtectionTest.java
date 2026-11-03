package com.userfront.config;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.security.Principal;
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
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.userfront.controller.AccountController;
import com.userfront.controller.AppointmentController;
import com.userfront.controller.TransferController;
import com.userfront.controller.UserController;
import com.userfront.dao.UserDao;
import com.userfront.domain.Appointment;
import com.userfront.domain.User;
import com.userfront.service.AccountService;
import com.userfront.service.AppointmentService;
import com.userfront.service.TransactionService;
import com.userfront.service.UserService;
import com.userfront.service.UserServiceImpl.UserSecurityService;

@RunWith(SpringRunner.class)
@WebMvcTest(controllers = {
        AccountController.class,
        AppointmentController.class,
        TransferController.class,
        UserController.class
})
@Import(SecurityConfig.class)
public class CsrfProtectionTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserSecurityService userSecurityService;

    @MockBean
    private UserDao userDao;

    @MockBean
    private UserService userService;

    @MockBean
    private AccountService accountService;

    @MockBean
    private TransactionService transactionService;

    @MockBean
    private AppointmentService appointmentService;

    @Before
    public void setUp() {
        User alice = new User();
        alice.setUsername("alice");
        when(userService.findByUsername("alice")).thenReturn(alice);
        when(transactionService.findRecipientList(any(Principal.class))).thenReturn(Collections.emptyList());
    }

    private static MockHttpServletRequestBuilder deposit() {
        return post("/account/deposit").param("accountType", "Primary").param("amount", "10")
                .with(user("alice"));
    }

    private static MockHttpServletRequestBuilder withdraw() {
        return post("/account/withdraw").param("accountType", "Primary").param("amount", "10")
                .with(user("alice"));
    }

    private static MockHttpServletRequestBuilder betweenAccounts() {
        return post("/transfer/betweenAccounts").param("transferFrom", "Primary").param("transferTo", "Savings")
                .param("amount", "10").with(user("alice"));
    }

    private static MockHttpServletRequestBuilder toSomeoneElse() {
        return post("/transfer/toSomeoneElse").param("recipientName", "bob").param("accountType", "Primary")
                .param("amount", "10").with(user("alice"));
    }

    private static MockHttpServletRequestBuilder recipientSave() {
        return post("/transfer/recipient/save").param("name", "bob").param("accountNumber", "123")
                .with(user("alice"));
    }

    private static MockHttpServletRequestBuilder recipientDelete() {
        return post("/transfer/recipient/delete").param("recipientName", "bob").with(user("alice"));
    }

    private static MockHttpServletRequestBuilder appointment() {
        return post("/appointment/create").param("dateString", "2030-01-01 10:00").param("location", "HQ")
                .param("description", "loan").with(user("alice"));
    }

    private static MockHttpServletRequestBuilder profile() {
        return post("/user/profile").param("username", "alice").param("email", "attacker@example.com")
                .with(user("alice"));
    }

    @Test
    public void formPostsWithoutCsrfTokenAreRejected() throws Exception {
        for (MockHttpServletRequestBuilder request : new MockHttpServletRequestBuilder[] {
                deposit(), withdraw(), betweenAccounts(), toSomeoneElse(), recipientSave(), recipientDelete(),
                appointment(), profile() }) {
            mockMvc.perform(request).andExpect(status().isForbidden());
        }

        verify(accountService, never()).deposit(anyString(), anyDouble(), any(Principal.class));
        verify(accountService, never()).withdraw(anyString(), anyDouble(), any(Principal.class));
        verify(transactionService, never()).betweenAccountsTransfer(any(), any(), any(), any(), any());
        verify(transactionService, never()).toSomeoneElseTransfer(any(), any(), any(), any(), any());
        verify(transactionService, never()).saveRecipient(any());
        verify(transactionService, never()).deleteRecipientByName(any());
        verify(appointmentService, never()).createAppointment(any(Appointment.class));
        verify(userService, never()).saveUser(any(User.class));
    }

    @Test
    public void formPostsWithInvalidCsrfTokenAreRejected() throws Exception {
        mockMvc.perform(deposit().with(csrf().useInvalidToken())).andExpect(status().isForbidden());
        mockMvc.perform(toSomeoneElse().with(csrf().useInvalidToken())).andExpect(status().isForbidden());

        verify(accountService, never()).deposit(anyString(), anyDouble(), any(Principal.class));
        verify(transactionService, never()).toSomeoneElseTransfer(any(), any(), any(), any(), any());
    }

    @Test
    public void formPostsWithValidCsrfTokenAreProcessed() throws Exception {
        mockMvc.perform(deposit().with(csrf())).andExpect(redirectedUrl("/userFront"));
        mockMvc.perform(withdraw().with(csrf())).andExpect(redirectedUrl("/userFront"));
        mockMvc.perform(appointment().with(csrf())).andExpect(redirectedUrl("/userFront"));
        mockMvc.perform(recipientDelete().with(csrf())).andExpect(status().isOk());

        verify(accountService).deposit(eq("Primary"), eq(10.0), any(Principal.class));
        verify(accountService).withdraw(anyString(), anyDouble(), any(Principal.class));
        verify(appointmentService).createAppointment(any(Appointment.class));
        verify(transactionService).deleteRecipientByName("bob");
    }

    @Test
    public void recipientDeleteIsNotReachableWithGet() throws Exception {
        mockMvc.perform(get("/transfer/recipient/delete").param("recipientName", "bob").with(user("alice")))
                .andExpect(status().isMethodNotAllowed());

        verify(transactionService, never()).deleteRecipientByName(any());
    }

    @Test
    public void loginRequiresCsrfToken() throws Exception {
        mockMvc.perform(post("/index").param("username", "alice").param("password", "pw"))
                .andExpect(status().isForbidden());
    }

    @Test
    public void logoutRequiresPostWithCsrfToken() throws Exception {
        mockMvc.perform(post("/logout").with(user("alice"))).andExpect(status().isForbidden());
        mockMvc.perform(get("/logout").with(user("alice")))
                .andExpect(status().is(not(302)));
        mockMvc.perform(post("/logout").with(user("alice")).with(csrf()))
                .andExpect(redirectedUrl("/index?logout"));
    }

    @Test
    public void renderedFormsCarryTheCsrfToken() throws Exception {
        for (String page : new String[] { "/account/deposit", "/account/withdraw", "/transfer/betweenAccounts",
                "/transfer/toSomeoneElse", "/transfer/recipient", "/appointment/create" }) {
            mockMvc.perform(get(page).with(user("alice")))
                    .andExpect(status().isOk())
                    .andExpect(content().string(containsString("name=\"_csrf\"")))
                    .andExpect(content().string(not(containsString("${_csrf"))));
        }
    }
}
