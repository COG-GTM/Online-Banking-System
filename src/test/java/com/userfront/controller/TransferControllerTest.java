package com.userfront.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.security.Principal;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.userfront.domain.User;
import com.userfront.dto.RecipientForm;
import com.userfront.service.RecipientNotFoundException;
import com.userfront.service.TransactionService;
import com.userfront.service.UserService;

@RunWith(MockitoJUnitRunner.class)
public class TransferControllerTest {

    @Mock
    private TransactionService transactionService;

    @Mock
    private UserService userService;

    @InjectMocks
    private TransferController transferController;

    private MockMvc mockMvc;
    private User bob;
    private final Principal principal = () -> "bob";

    @Before
    public void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(transferController).build();
        bob = new User();
        bob.setUsername("bob");
        when(userService.findByUsername("bob")).thenReturn(bob);
    }

    @Test
    public void saveBindsOnlyRecipientFieldsAndPassesCurrentUser() throws Exception {
        mockMvc.perform(post("/transfer/recipient/save").principal(principal)
                .param("name", "Payee")
                .param("accountNumber", "999")
                .param("user.username", "alice")
                .param("user.userId", "1"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/transfer/recipient"));

        ArgumentCaptor<RecipientForm> form = ArgumentCaptor.forClass(RecipientForm.class);
        verify(transactionService).saveRecipient(form.capture(), eq(bob));
        assertNull(form.getValue().getId());
        assertEquals("Payee", form.getValue().getName());
        assertEquals("999", form.getValue().getAccountNumber());
    }

    @Test
    public void saveWithForeignRecipientIdReturns404() throws Exception {
        when(transactionService.saveRecipient(any(RecipientForm.class), eq(bob)))
                .thenThrow(new RecipientNotFoundException(7L));

        mockMvc.perform(post("/transfer/recipient/save").principal(principal)
                .param("id", "7")
                .param("name", "Hijack")
                .param("accountNumber", "666"))
                .andExpect(status().isNotFound());
    }

    @Test
    public void editOfOtherUsersRecipientNameReturns404() throws Exception {
        when(transactionService.findRecipientByName(eq("Landlord"), any(Principal.class)))
                .thenThrow(new RecipientNotFoundException("Landlord"));

        mockMvc.perform(get("/transfer/recipient/edit").principal(principal)
                .param("recipientName", "Landlord"))
                .andExpect(status().isNotFound());
    }
}
