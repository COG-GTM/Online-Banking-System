package com.userfront.service.UserServiceImpl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.security.Principal;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
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
    public void findRecipientByIdIsScopedToCaller() {
        Recipient own = new Recipient();
        when(recipientDao.findByIdAndUserUsername(7L, "alice")).thenReturn(own);

        assertSame(own, transactionService.findRecipientById(7L, alice));
    }

    @Test(expected = RecipientNotFoundException.class)
    public void findRecipientByIdRejectsOtherUsersRecipient() {
        when(recipientDao.findByIdAndUserUsername(42L, "alice")).thenReturn(null);

        transactionService.findRecipientById(42L, alice);
    }

    @Test(expected = RecipientNotFoundException.class)
    public void findRecipientByIdRejectsMissingId() {
        transactionService.findRecipientById(null, alice);
    }

    @Test
    public void deleteRecipientByIdIsScopedToCaller() {
        when(recipientDao.deleteByIdAndUserUsername(7L, "alice")).thenReturn(1L);

        transactionService.deleteRecipientById(7L, alice);

        verify(recipientDao).deleteByIdAndUserUsername(7L, "alice");
    }

    @Test(expected = RecipientNotFoundException.class)
    public void deleteRecipientByIdRejectsOtherUsersRecipient() {
        when(recipientDao.deleteByIdAndUserUsername(42L, "alice")).thenReturn(0L);

        transactionService.deleteRecipientById(42L, alice);
    }

    @Test
    public void saveNewRecipientIsOwnedByCaller() {
        User user = new User();
        when(userService.findByUsername("alice")).thenReturn(user);
        Recipient form = form(null, "Landlord", "ACC-1");

        transactionService.saveRecipient(form, alice);

        ArgumentCaptor<Recipient> saved = ArgumentCaptor.forClass(Recipient.class);
        verify(recipientDao).save(saved.capture());
        assertSame(user, saved.getValue().getUser());
        assertEquals("ACC-1", saved.getValue().getAccountNumber());
    }

    @Test
    public void saveExistingRecipientUpdatesTheOwnedRow() {
        User user = new User();
        Recipient owned = form(7L, "Old", "OLD");
        owned.setUser(user);
        when(recipientDao.findByIdAndUserUsername(7L, "alice")).thenReturn(owned);

        transactionService.saveRecipient(form(7L, "New", "NEW"), alice);

        verify(recipientDao).save(owned);
        assertEquals("New", owned.getName());
        assertEquals("NEW", owned.getAccountNumber());
        assertSame(user, owned.getUser());
    }

    @Test
    public void saveRejectsOtherUsersRecipientId() {
        when(recipientDao.findByIdAndUserUsername(42L, "alice")).thenReturn(null);

        try {
            transactionService.saveRecipient(form(42L, "Hijack", "ATTACKER"), alice);
        } catch (RecipientNotFoundException expected) {
            verify(recipientDao, never()).save(any(Recipient.class));
            return;
        }
        throw new AssertionError("expected RecipientNotFoundException");
    }

    private static Recipient form(Long id, String name, String accountNumber) {
        Recipient r = new Recipient();
        r.setId(id);
        r.setName(name);
        r.setAccountNumber(accountNumber);
        return r;
    }
}
