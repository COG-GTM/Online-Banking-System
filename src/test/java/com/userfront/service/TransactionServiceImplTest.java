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
    public void deleteRecipientByNameIsScopedToPrincipal() {
        Principal alice = () -> "alice";

        transactionService.deleteRecipientByName("Bob", alice);

        verify(recipientDao).deleteByNameAndUserUsername("Bob", "alice");
        verifyNoMoreInteractions(recipientDao);
    }
}
