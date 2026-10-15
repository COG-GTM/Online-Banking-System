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
import com.userfront.service.UserService;
import com.userfront.service.exception.RecipientNotFoundException;

@RunWith(MockitoJUnitRunner.class)
public class TransactionServiceImplTest {

    private final Principal alice = () -> "alice";

    @Mock
    private RecipientDao recipientDao;

    @Mock
    private UserService userService;

    @InjectMocks
    private TransactionServiceImpl transactionService;

    @Test
    public void findRecipientByNameIsScopedToCaller() {
        Recipient own = new Recipient();
        when(recipientDao.findFirstByNameAndUserUsername("Landlord", "alice")).thenReturn(own);

        assertSame(own, transactionService.findRecipientByName("Landlord", alice));
    }

    @Test(expected = RecipientNotFoundException.class)
    public void findRecipientByNameRejectsOtherUsersRecipient() {
        when(recipientDao.findFirstByNameAndUserUsername("BobsPayee", "alice")).thenReturn(null);

        transactionService.findRecipientByName("BobsPayee", alice);
    }

    @Test
    public void deleteRecipientByNameIsScopedToCaller() {
        when(recipientDao.deleteByNameAndUserUsername("Landlord", "alice")).thenReturn(1L);

        transactionService.deleteRecipientByName("Landlord", alice);

        verify(recipientDao).deleteByNameAndUserUsername("Landlord", "alice");
    }

    @Test(expected = RecipientNotFoundException.class)
    public void deleteRecipientByNameRejectsUnknownName() {
        when(recipientDao.deleteByNameAndUserUsername("BobsPayee", "alice")).thenReturn(0L);

        transactionService.deleteRecipientByName("BobsPayee", alice);
    }

    @Test
    public void saveNewRecipientIsOwnedByCaller() {
        User user = new User();
        Recipient recipient = new Recipient();
        when(userService.findByUsername("alice")).thenReturn(user);
        when(recipientDao.save(recipient)).thenReturn(recipient);

        transactionService.saveRecipient(recipient, alice);

        assertSame(user, recipient.getUser());
        verify(recipientDao).save(recipient);
    }

    @Test
    public void saveExistingOwnRecipientIsAllowed() {
        Recipient recipient = new Recipient();
        recipient.setId(7L);
        when(recipientDao.findByIdAndUserUsername(7L, "alice")).thenReturn(new Recipient());

        transactionService.saveRecipient(recipient, alice);

        verify(recipientDao).save(recipient);
    }

    @Test
    public void saveRejectsOtherUsersRecipientId() {
        Recipient recipient = new Recipient();
        recipient.setId(42L);
        when(recipientDao.findByIdAndUserUsername(42L, "alice")).thenReturn(null);

        try {
            transactionService.saveRecipient(recipient, alice);
        } catch (RecipientNotFoundException expected) {
            verify(recipientDao, never()).save(any(Recipient.class));
            return;
        }
        throw new AssertionError("expected RecipientNotFoundException");
    }
}
