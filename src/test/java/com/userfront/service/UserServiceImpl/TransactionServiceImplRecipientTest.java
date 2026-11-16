package com.userfront.service.UserServiceImpl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.security.Principal;
import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import com.userfront.dao.RecipientDao;
import com.userfront.domain.Recipient;
import com.userfront.domain.User;
import com.userfront.service.RecipientLimitExceededException;

public class TransactionServiceImplRecipientTest {

    private RecipientDao recipientDao;
    private TransactionServiceImpl service;

    @Before
    public void setUp() throws Exception {
        recipientDao = mock(RecipientDao.class);
        service = new TransactionServiceImpl();
        Field field = TransactionServiceImpl.class.getDeclaredField("recipientDao");
        field.setAccessible(true);
        field.set(service, recipientDao);
    }

    @Test
    public void findRecipientListQueriesOnlyTheCallersRecipients() {
        Recipient own = recipientOwnedBy("alice");
        when(recipientDao.findByUserUsername("alice")).thenReturn(Collections.singletonList(own));
        Principal principal = () -> "alice";

        List<Recipient> result = service.findRecipientList(principal);

        assertEquals(Collections.singletonList(own), result);
        verify(recipientDao, never()).findAll();
    }

    @Test
    public void saveRecipientRejectsRecipientWithoutOwner() {
        Recipient recipient = new Recipient();
        recipient.setName("x");

        try {
            service.saveRecipient(recipient);
            fail("expected IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
            // expected
        }
        verify(recipientDao, never()).save(recipient);
    }

    @Test
    public void saveRecipientRejectsNewRecipientAtLimit() {
        Recipient recipient = recipientOwnedBy("alice");
        when(recipientDao.countByUserUsername("alice"))
                .thenReturn((long) TransactionServiceImpl.MAX_RECIPIENTS_PER_USER);

        try {
            service.saveRecipient(recipient);
            fail("expected RecipientLimitExceededException");
        } catch (RecipientLimitExceededException expected) {
            // expected
        }
        verify(recipientDao, never()).save(recipient);
    }

    @Test
    public void saveRecipientAllowsNewRecipientBelowLimit() {
        Recipient recipient = recipientOwnedBy("alice");
        when(recipientDao.countByUserUsername("alice"))
                .thenReturn((long) TransactionServiceImpl.MAX_RECIPIENTS_PER_USER - 1);
        when(recipientDao.save(recipient)).thenReturn(recipient);

        assertSame(recipient, service.saveRecipient(recipient));
    }

    @Test
    public void saveRecipientAllowsEditingExistingRecipientAtLimit() {
        Recipient recipient = recipientOwnedBy("alice");
        recipient.setId(5L);
        when(recipientDao.countByUserUsername("alice"))
                .thenReturn((long) TransactionServiceImpl.MAX_RECIPIENTS_PER_USER);
        when(recipientDao.save(recipient)).thenReturn(recipient);

        assertSame(recipient, service.saveRecipient(recipient));
    }

    private static Recipient recipientOwnedBy(String username) {
        User user = new User();
        user.setUsername(username);
        Recipient recipient = new Recipient();
        recipient.setName("payee");
        recipient.setUser(user);
        return recipient;
    }
}
