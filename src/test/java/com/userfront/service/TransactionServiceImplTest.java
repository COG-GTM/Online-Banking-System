package com.userfront.service;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import java.security.Principal;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.userfront.dao.RecipientDao;
import com.userfront.service.UserServiceImpl.TransactionServiceImpl;

@RunWith(MockitoJUnitRunner.class)
public class TransactionServiceImplTest {

    @Mock
    private RecipientDao recipientDao;

    @InjectMocks
    private TransactionServiceImpl transactionService;

    @Test
    public void deleteRecipientByIdIsScopedToPrincipal() {
        Principal alice = () -> "alice";

        transactionService.deleteRecipientById(42L, alice);

        verify(recipientDao).deleteByIdAndUserUsername(42L, "alice");
        verifyNoMoreInteractions(recipientDao);
    }
}
