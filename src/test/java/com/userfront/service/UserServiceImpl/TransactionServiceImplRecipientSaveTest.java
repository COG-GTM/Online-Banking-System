package com.userfront.service.UserServiceImpl;

import static org.junit.Assert.assertSame;
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
import com.userfront.domain.User;
import com.userfront.service.RecipientNotFoundException;
import com.userfront.service.UserService;

@RunWith(MockitoJUnitRunner.class)
public class TransactionServiceImplRecipientSaveTest {

    @Mock
    private RecipientDao recipientDao;

    @Mock
    private UserService userService;

    @InjectMocks
    private TransactionServiceImpl transactionService;

    private final Principal alice = () -> "alice";

    @Test
    public void createsNewRecipientOwnedByCaller() {
        User aliceUser = new User();
        when(userService.findByUsername("alice")).thenReturn(aliceUser);
        Recipient recipient = new Recipient();

        transactionService.saveRecipient(recipient, alice);

        assertSame(aliceUser, recipient.getUser());
        verify(recipientDao).save(recipient);
    }

    @Test
    public void updatesRecipientOwnedByCaller() {
        Recipient recipient = new Recipient();
        recipient.setId(7L);
        when(recipientDao.findByIdAndUserUsername(7L, "alice")).thenReturn(new Recipient());

        transactionService.saveRecipient(recipient, alice);

        verify(recipientDao).save(recipient);
    }

    @Test(expected = RecipientNotFoundException.class)
    public void refusesToReassignAnotherUsersRecipient() {
        Recipient recipient = new Recipient();
        recipient.setId(8L);
        when(recipientDao.findByIdAndUserUsername(8L, "alice")).thenReturn(null);

        try {
            transactionService.saveRecipient(recipient, alice);
        } finally {
            verify(recipientDao, never()).save(any(Recipient.class));
        }
    }
}
