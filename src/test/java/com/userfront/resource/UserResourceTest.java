package com.userfront.resource;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Arrays;
import java.util.Collections;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.userfront.domain.PrimaryTransaction;
import com.userfront.domain.SavingsTransaction;
import com.userfront.service.TransactionService;
import com.userfront.service.UserService;

@RunWith(MockitoJUnitRunner.class)
public class UserResourceTest {

    @Mock
    private TransactionService transactionService;

    @Mock
    private UserService userService;

    @InjectMocks
    private UserResource userResource;

    private MockMvc mockMvc;

    @Before
    public void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(userResource).build();
    }

    @Test
    public void savingsTransactionsDefaultToFirstBoundedPage() throws Exception {
        when(transactionService.findSavingsTransactionPage("alice", 0, UserResource.DEFAULT_PAGE_SIZE))
                .thenReturn(new PageImpl<>(Arrays.asList(new SavingsTransaction(), new SavingsTransaction()),
                        PageRequest.of(0, UserResource.DEFAULT_PAGE_SIZE), 120));

        mockMvc.perform(get("/api/user/savings/transaction").param("username", "alice"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(header().string("X-Total-Count", "120"))
                .andExpect(header().string("X-Total-Pages", "3"))
                .andExpect(header().string("X-Page", "0"))
                .andExpect(header().string("X-Page-Size", String.valueOf(UserResource.DEFAULT_PAGE_SIZE)));

        verify(transactionService, never()).findSavingsTransactionList(anyString());
    }

    @Test
    public void savingsTransactionsHonourRequestedPage() throws Exception {
        when(transactionService.findSavingsTransactionPage("alice", 2, 10))
                .thenReturn(new PageImpl<>(Collections.singletonList(new SavingsTransaction()),
                        PageRequest.of(2, 10), 21));

        mockMvc.perform(get("/api/user/savings/transaction")
                .param("username", "alice").param("page", "2").param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(header().string("X-Total-Count", "21"))
                .andExpect(header().string("X-Page", "2"));
    }

    @Test
    public void primaryTransactionsArePaged() throws Exception {
        when(transactionService.findPrimaryTransactionPage("bob", 1, 25))
                .thenReturn(new PageImpl<>(Collections.singletonList(new PrimaryTransaction()),
                        PageRequest.of(1, 25), 26));

        mockMvc.perform(get("/api/user/primary/transaction")
                .param("username", "bob").param("page", "1").param("size", "25"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(header().string("X-Total-Count", "26"))
                .andExpect(header().string("X-Total-Pages", "2"));

        verify(transactionService, never()).findPrimaryTransactionList(anyString());
    }

    @Test
    public void rejectsOversizedOrNegativePaging() throws Exception {
        mockMvc.perform(get("/api/user/savings/transaction")
                .param("username", "alice").param("size", String.valueOf(UserResource.MAX_PAGE_SIZE + 1)))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/user/savings/transaction").param("username", "alice").param("size", "0"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/user/primary/transaction").param("username", "bob").param("page", "-1"))
                .andExpect(status().isBadRequest());

        verify(transactionService, never()).findSavingsTransactionPage(anyString(), anyInt(), anyInt());
        verify(transactionService, never()).findPrimaryTransactionPage(anyString(), anyInt(), anyInt());
    }
}
