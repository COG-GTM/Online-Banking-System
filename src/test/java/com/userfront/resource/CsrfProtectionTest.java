package com.userfront.resource;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import com.userfront.config.SecurityConfig;
import com.userfront.controller.AccountController;
import com.userfront.controller.TransferController;
import com.userfront.service.AccountService;
import com.userfront.service.AppointmentService;
import com.userfront.service.TransactionService;
import com.userfront.service.UserService;
import com.userfront.service.UserServiceImpl.UserSecurityService;

@WebMvcTest(controllers = {UserResource.class, AppointmentResource.class, TransferController.class, AccountController.class})
@Import(SecurityConfig.class)
public class CsrfProtectionTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @MockBean
    private TransactionService transactionService;

    @MockBean
    private AppointmentService appointmentService;

    @MockBean
    private AccountService accountService;

    @MockBean
    private UserSecurityService userSecurityService;

    @Test
    @WithMockUser(roles = "ADMIN")
    public void disableUserRejectsGet() throws Exception {
        mockMvc.perform(get("/api/user/bob/disable")).andExpect(status().isMethodNotAllowed());
        verify(userService, never()).disableUser("bob");
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    public void disableUserRejectsPostWithoutCsrfToken() throws Exception {
        mockMvc.perform(post("/api/user/bob/disable")).andExpect(status().isForbidden());
        verify(userService, never()).disableUser("bob");
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    public void disableUserAcceptsPostWithCsrfToken() throws Exception {
        mockMvc.perform(post("/api/user/bob/disable").with(csrf())).andExpect(status().isOk());
        verify(userService).disableUser("bob");
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    public void enableUserRejectsGet() throws Exception {
        mockMvc.perform(get("/api/user/bob/enable")).andExpect(status().isMethodNotAllowed());
        verify(userService, never()).enableUser("bob");
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    public void confirmAppointmentRejectsGetAndTokenlessPost() throws Exception {
        mockMvc.perform(get("/api/appointment/1/confirm")).andExpect(status().isMethodNotAllowed());
        mockMvc.perform(post("/api/appointment/1/confirm")).andExpect(status().isForbidden());
        verify(appointmentService, never()).confirmAppointment(1L);
    }

    @Test
    @WithMockUser
    public void recipientDeleteRejectsGetAndTokenlessPost() throws Exception {
        mockMvc.perform(get("/transfer/recipient/delete").param("recipientName", "Landlord"))
                .andExpect(status().isMethodNotAllowed());
        mockMvc.perform(post("/transfer/recipient/delete").param("recipientName", "Landlord"))
                .andExpect(status().isForbidden());
        verify(transactionService, never()).deleteRecipientByName("Landlord");
    }

    @Test
    @WithMockUser
    public void withdrawRejectsTokenlessPost() throws Exception {
        mockMvc.perform(post("/account/withdraw").param("accountType", "Primary").param("amount", "5000"))
                .andExpect(status().isForbidden());
        verify(accountService, never()).withdraw(anyString(), anyDouble(), any());
    }
}
