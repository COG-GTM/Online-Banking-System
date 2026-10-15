package com.userfront.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.security.Principal;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.userfront.service.RecipientNotFoundException;
import com.userfront.service.TransactionService;
import com.userfront.service.UserService;

@RunWith(MockitoJUnitRunner.class)
public class TransferControllerRecipientDeleteTest {

    @Mock
    private TransactionService transactionService;

    @Mock
    private UserService userService;

    @InjectMocks
    private TransferController controller;

    private MockMvc mockMvc;

    private final Principal alice = () -> "alice";

    @Before
    public void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    public void getIsNotAllowed() throws Exception {
        mockMvc.perform(get("/transfer/recipient/delete").param("recipientName", "Bob").principal(alice))
                .andExpect(status().isMethodNotAllowed());

        verify(transactionService, never()).deleteRecipient(any(), any());
    }

    @Test
    public void postDeletesByIdForCaller() throws Exception {
        mockMvc.perform(post("/transfer/recipient/delete").param("recipientId", "7").principal(alice))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/transfer/recipient"));

        verify(transactionService).deleteRecipient(eq(7L), any(Principal.class));
    }

    @Test
    public void foreignRecipientIsNotFound() throws Exception {
        doThrow(new RecipientNotFoundException()).when(transactionService).deleteRecipient(eq(8L), any(Principal.class));

        mockMvc.perform(post("/transfer/recipient/delete").param("recipientId", "8").principal(alice))
                .andExpect(status().isNotFound());
    }
}
