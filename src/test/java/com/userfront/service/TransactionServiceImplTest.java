package com.userfront.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
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
import com.userfront.service.UserServiceImpl.TransactionServiceImpl;

@RunWith(MockitoJUnitRunner.class)
public class TransactionServiceImplTest {

    @Mock
    private RecipientDao recipientDao;

    @Mock
    private UserService userService;

    @InjectMocks
    private TransactionServiceImpl transactionService;

    private final Principal alice = () -> "alice";

    @Test
    public void findRecipientByIdIsScopedToPrincipal() {
        Recipient owned = recipient(7L, "Bob", user("alice"));
        when(recipientDao.findByIdAndUserUsername(7L, "alice")).thenReturn(owned);

        assertSame(owned, transactionService.findRecipientById(7L, alice));
    }

    @Test(expected = RecipientNotFoundException.class)
    public void findRecipientByIdRejectsOtherUsersRecipient() {
        when(recipientDao.findByIdAndUserUsername(9L, "alice")).thenReturn(null);

        transactionService.findRecipientById(9L, alice);
    }

    @Test
    public void deleteRecipientByIdDeletesOwnedRecipient() {
        Recipient owned = recipient(7L, "Bob", user("alice"));
        when(recipientDao.findByIdAndUserUsername(7L, "alice")).thenReturn(owned);

        transactionService.deleteRecipientById(7L, alice);

        verify(recipientDao).delete(owned);
    }

    @Test
    public void deleteRecipientByIdRejectsOtherUsersRecipient() {
        when(recipientDao.findByIdAndUserUsername(9L, "alice")).thenReturn(null);

        try {
            transactionService.deleteRecipientById(9L, alice);
        } catch (RecipientNotFoundException expected) {
            verify(recipientDao, never()).delete(any(Recipient.class));
            return;
        }
        throw new AssertionError("expected RecipientNotFoundException");
    }

    @Test
    public void saveNewRecipientIgnoresClientUserAndBindsPrincipal() {
        User aliceUser = user("alice");
        when(userService.findByUsername("alice")).thenReturn(aliceUser);
        when(recipientDao.save(any(Recipient.class))).thenAnswer(i -> i.getArgument(0));

        Recipient submitted = recipient(null, "Carol", user("mallory"));
        submitted.setAccountNumber("123");

        Recipient saved = transactionService.saveRecipient(submitted, alice);

        assertNull(saved.getId());
        assertSame(aliceUser, saved.getUser());
        assertEquals("Carol", saved.getName());
        assertEquals("123", saved.getAccountNumber());
    }

    @Test
    public void saveExistingRecipientUpdatesOwnedRecordOnly() {
        User aliceUser = user("alice");
        Recipient owned = recipient(7L, "Bob", aliceUser);
        when(recipientDao.findByIdAndUserUsername(7L, "alice")).thenReturn(owned);
        when(recipientDao.save(any(Recipient.class))).thenAnswer(i -> i.getArgument(0));

        Recipient submitted = recipient(7L, "Bobby", null);
        submitted.setAccountNumber("999");
        transactionService.saveRecipient(submitted, alice);

        ArgumentCaptor<Recipient> captor = ArgumentCaptor.forClass(Recipient.class);
        verify(recipientDao).save(captor.capture());
        assertSame(owned, captor.getValue());
        assertSame(aliceUser, captor.getValue().getUser());
        assertEquals("Bobby", captor.getValue().getName());
        assertEquals("999", captor.getValue().getAccountNumber());
    }

    @Test
    public void saveWithOtherUsersRecipientIdIsRejected() {
        when(recipientDao.findByIdAndUserUsername(9L, "alice")).thenReturn(null);

        try {
            transactionService.saveRecipient(recipient(9L, "Takeover", null), alice);
        } catch (RecipientNotFoundException expected) {
            verify(recipientDao, never()).save(any(Recipient.class));
            return;
        }
        throw new AssertionError("expected RecipientNotFoundException");
    }

    private static User user(String username) {
        User user = new User();
        user.setUsername(username);
        return user;
    }

    private static Recipient recipient(Long id, String name, User user) {
        Recipient recipient = new Recipient();
        recipient.setId(id);
        recipient.setName(name);
        recipient.setUser(user);
        return recipient;
    }
}
