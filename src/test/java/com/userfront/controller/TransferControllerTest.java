package com.userfront.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
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

    private final Principal alice = () -> "alice";

    @Before
    public void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(transferController).build();
    }

    @Test
    public void deleteViaGetIsRejected() throws Exception {
        mockMvc.perform(get("/transfer/recipient/delete").param("recipientId", "42").principal(alice))
                .andExpect(status().isMethodNotAllowed());

        verify(transactionService, never()).deleteRecipientById(anyLong(), any(Principal.class));
    }

    @Test
    public void deleteViaPostIsScopedToPrincipal() throws Exception {
        mockMvc.perform(post("/transfer/recipient/delete").param("recipientId", "42").principal(alice))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/transfer/recipient"));

        verify(transactionService).deleteRecipientById(42L, alice);
    }
}
