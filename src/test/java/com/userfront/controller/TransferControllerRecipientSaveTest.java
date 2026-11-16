package com.userfront.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.security.Principal;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.userfront.domain.User;
import com.userfront.dto.RecipientForm;
import com.userfront.service.RecipientNotFoundException;
import com.userfront.service.TransactionService;
import com.userfront.service.UserService;

public class TransferControllerRecipientSaveTest {

    private static final Principal MALLORY = new UsernamePasswordAuthenticationToken("mallory", "n/a");

    @Mock
    private TransactionService transactionService;

    @Mock
    private UserService userService;

    @InjectMocks
    private TransferController controller;

    private MockMvc mockMvc;
    private User mallory;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
        mallory = new User();
        mallory.setUsername("mallory");
        when(userService.findByUsername("mallory")).thenReturn(mallory);
    }

    @Test
    public void saveBindsOnlyEditableFieldsAndPassesCallerAsOwner() throws Exception {
        mockMvc.perform(post("/transfer/recipient/save").principal(MALLORY)
                .param("name", "Landlord")
                .param("email", "l@example.com")
                .param("phone", "555")
                .param("accountNumber", "999")
                .param("description", "rent")
                .param("user.username", "alice")
                .param("user.userId", "1"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/transfer/recipient"));

        ArgumentCaptor<RecipientForm> captor = ArgumentCaptor.forClass(RecipientForm.class);
        verify(transactionService).saveRecipient(captor.capture(), eq(mallory));
        RecipientForm form = captor.getValue();
        assertNull(form.getId());
        assertEquals("Landlord", form.getName());
        assertEquals("999", form.getAccountNumber());
    }

    @Test
    public void saveWithForeignRecipientIdIsNotFound() throws Exception {
        when(transactionService.saveRecipient(any(RecipientForm.class), eq(mallory)))
                .thenThrow(new RecipientNotFoundException(42L));

        mockMvc.perform(post("/transfer/recipient/save").principal(MALLORY)
                .param("id", "42")
                .param("name", "Mallory's"))
                .andExpect(status().isNotFound());
    }
}
