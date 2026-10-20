package com.userfront.service;

import static org.junit.Assert.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.security.Principal;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.userfront.dao.RecipientDao;
import com.userfront.domain.Recipient;
import com.userfront.domain.User;
import com.userfront.exception.RecipientNotFoundException;
import com.userfront.service.UserServiceImpl.TransactionServiceImpl;

@RunWith(MockitoJUnitRunner.class)
public class TransactionServiceImplRecipientTest {

    @Mock
    private RecipientDao recipientDao;

    @Mock
    private UserService userService;

    @InjectMocks
    private TransactionServiceImpl transactionService;

    private final Principal attackerPrincipal = () -> "attacker";
    private User attacker;

    @Before
    public void setUp() {
        attacker = new User();
        attacker.setUsername("attacker");
        when(userService.findByUsername("attacker")).thenReturn(attacker);
    }

    @Test(expected = RecipientNotFoundException.class)
    public void findRecipientByNameDoesNotReturnOtherUsersRecipient() {
        when(recipientDao.findByNameAndUser("Bob", attacker)).thenReturn(null);

        transactionService.findRecipientByName("Bob", attackerPrincipal);
    }

    @Test
    public void findRecipientByNameReturnsOwnRecipient() {
        Recipient own = new Recipient();
        when(recipientDao.findByNameAndUser("Bob", attacker)).thenReturn(own);

        assertSame(own, transactionService.findRecipientByName("Bob", attackerPrincipal));
    }

    @Test
    public void deleteRecipientByNameIsScopedToPrincipal() {
        transactionService.deleteRecipientByName("Bob", attackerPrincipal);

        verify(recipientDao).deleteByNameAndUser("Bob", attacker);
    }

    @Test
    public void saveRecipientRejectsOtherUsersRecipientId() {
        Recipient forged = new Recipient();
        forged.setId(42L);
        when(recipientDao.findByIdAndUser(42L, attacker)).thenReturn(null);

        try {
            transactionService.saveRecipient(forged, attackerPrincipal);
        } catch (RecipientNotFoundException expected) {
            verify(recipientDao, never()).save(any(Recipient.class));
            return;
        }
        throw new AssertionError("Expected RecipientNotFoundException");
    }

    @Test
    public void saveRecipientAllowsOwnRecipientId() {
        Recipient own = new Recipient();
        own.setId(7L);
        when(recipientDao.findByIdAndUser(7L, attacker)).thenReturn(own);

        transactionService.saveRecipient(own, attackerPrincipal);

        assertSame(attacker, own.getUser());
        verify(recipientDao).save(own);
    }

    @Test
    public void saveNewRecipientAssignsPrincipalAsOwner() {
        Recipient created = new Recipient();

        transactionService.saveRecipient(created, attackerPrincipal);

        assertSame(attacker, created.getUser());
        verify(recipientDao).save(created);
    }
}
