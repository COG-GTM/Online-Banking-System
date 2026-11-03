package com.userfront.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.security.Principal;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.userfront.dao.RecipientDao;
import com.userfront.domain.Recipient;
import com.userfront.service.UserServiceImpl.TransactionServiceImpl;

@RunWith(MockitoJUnitRunner.class)
public class TransactionServiceImplTest {

    @Mock
    private RecipientDao recipientDao;

    @InjectMocks
    private TransactionServiceImpl transactionService;

    private final Principal alice = () -> "alice";

    @Test
    public void deletesRecipientOwnedByPrincipal() {
        Recipient owned = new Recipient();
        when(recipientDao.findByIdAndUserUsername(7L, "alice")).thenReturn(owned);

        transactionService.deleteRecipientById(7L, alice);

        verify(recipientDao).delete(owned);
    }

    @Test(expected = RecipientNotFoundException.class)
    public void refusesToDeleteRecipientOwnedBySomeoneElse() {
        when(recipientDao.findByIdAndUserUsername(9L, "alice")).thenReturn(null);

        try {
            transactionService.deleteRecipientById(9L, alice);
        } finally {
            verify(recipientDao, never()).delete(any(Recipient.class));
        }
    }
}
