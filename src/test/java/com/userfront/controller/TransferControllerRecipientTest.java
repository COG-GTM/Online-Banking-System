package com.userfront.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.security.Principal;
import java.util.Collections;

import org.junit.Before;
import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.view.InternalResourceViewResolver;

import com.userfront.dao.RecipientDao;
import com.userfront.domain.Recipient;
import com.userfront.domain.User;
import com.userfront.service.TransactionService;
import com.userfront.service.UserService;
import com.userfront.service.UserServiceImpl.TransactionServiceImpl;

public class TransferControllerRecipientTest {

    private RecipientDao recipientDao;
    private MockMvc mockMvc;
    private User attacker;
    private final Principal attackerPrincipal = () -> "attacker";

    @Before
    public void setUp() {
        recipientDao = mock(RecipientDao.class);
        UserService userService = mock(UserService.class);

        attacker = new User();
        attacker.setUsername("attacker");
        when(userService.findByUsername("attacker")).thenReturn(attacker);
        when(recipientDao.findAll()).thenReturn(Collections.emptyList());

        TransactionService transactionService = new TransactionServiceImpl();
        ReflectionTestUtils.setField(transactionService, "recipientDao", recipientDao);
        ReflectionTestUtils.setField(transactionService, "userService", userService);

        TransferController controller = new TransferController();
        ReflectionTestUtils.setField(controller, "transactionService", transactionService);
        ReflectionTestUtils.setField(controller, "userService", userService);

        InternalResourceViewResolver viewResolver = new InternalResourceViewResolver();
        viewResolver.setPrefix("/templates/");
        viewResolver.setSuffix(".html");
        mockMvc = MockMvcBuilders.standaloneSetup(controller).setViewResolvers(viewResolver).build();
    }

    @Test
    public void editOtherUsersRecipientReturnsNotFound() throws Exception {
        mockMvc.perform(get("/transfer/recipient/edit").param("recipientName", "Bob").principal(attackerPrincipal))
                .andExpect(status().isNotFound());

        verify(recipientDao).findByNameAndUser("Bob", attacker);
    }

    @Test
    public void deleteViaGetIsNotAllowed() throws Exception {
        mockMvc.perform(get("/transfer/recipient/delete").param("recipientName", "Bob").principal(attackerPrincipal))
                .andExpect(status().isMethodNotAllowed());

        verify(recipientDao, never()).deleteByNameAndUser(anyString(), any(User.class));
    }

    @Test
    public void deleteOnlyAffectsPrincipalsRecipients() throws Exception {
        mockMvc.perform(post("/transfer/recipient/delete").param("recipientName", "Bob").principal(attackerPrincipal))
                .andExpect(status().isOk());

        verify(recipientDao).deleteByNameAndUser("Bob", attacker);
    }

    @Test
    public void saveWithOtherUsersRecipientIdReturnsNotFound() throws Exception {
        mockMvc.perform(post("/transfer/recipient/save")
                .param("id", "42")
                .param("name", "Hijacked")
                .principal(attackerPrincipal))
                .andExpect(status().isNotFound());

        verify(recipientDao, never()).save(any(Recipient.class));
    }

    @Test
    public void transferToOtherUsersRecipientReturnsNotFound() throws Exception {
        mockMvc.perform(post("/transfer/toSomeoneElse")
                .param("recipientName", "Bob")
                .param("accountType", "Primary")
                .param("amount", "100")
                .principal(attackerPrincipal))
                .andExpect(status().isNotFound());
    }
}
