package com.userfront.service.UserServiceImpl;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;

import java.security.Principal;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.userfront.dao.RecipientDao;
import com.userfront.domain.Recipient;
import com.userfront.service.RecipientNotFoundException;

@RunWith(MockitoJUnitRunner.class)
public class TransactionServiceImplRecipientDeleteTest {

    @Mock
    private RecipientDao recipientDao;

    @InjectMocks
    private TransactionServiceImpl transactionService;

    private final Principal alice = () -> "alice";

    @Test
    public void deletesRecipientOwnedByCaller() {
        Recipient owned = new Recipient();
        owned.setId(7L);
        when(recipientDao.findByIdAndUserUsername(7L, "alice")).thenReturn(owned);

        transactionService.deleteRecipient(7L, alice);

        verify(recipientDao).delete(owned);
    }

    @Test(expected = RecipientNotFoundException.class)
    public void refusesToDeleteAnotherUsersRecipient() {
        when(recipientDao.findByIdAndUserUsername(8L, "alice")).thenReturn(null);

        try {
            transactionService.deleteRecipient(8L, alice);
        } finally {
            verify(recipientDao, never()).delete(any(Recipient.class));
        }
    }
}
