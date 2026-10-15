package com.userfront.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.userfront.dao.RecipientDao;
import com.userfront.domain.Recipient;
import com.userfront.domain.User;
import com.userfront.dto.RecipientForm;
import com.userfront.service.UserServiceImpl.TransactionServiceImpl;

import java.security.Principal;

@RunWith(MockitoJUnitRunner.class)
public class TransactionServiceImplTest {

    @Mock
    private RecipientDao recipientDao;

    @InjectMocks
    private TransactionServiceImpl transactionService;

    private User alice;
    private User bob;

    @Before
    public void setUp() {
        alice = new User();
        alice.setUsername("alice");
        bob = new User();
        bob.setUsername("bob");
    }

    private RecipientForm form(Long id) {
        RecipientForm form = new RecipientForm();
        form.setId(id);
        form.setName("Payee");
        form.setEmail("payee@example.com");
        form.setPhone("555-0100");
        form.setAccountNumber("999");
        form.setDescription("rent");
        return form;
    }

    @Test
    public void newRecipientIsCreatedForCurrentUser() {
        when(recipientDao.save(any(Recipient.class))).then(i -> i.getArgument(0));

        transactionService.saveRecipient(form(null), alice);

        ArgumentCaptor<Recipient> saved = ArgumentCaptor.forClass(Recipient.class);
        verify(recipientDao).save(saved.capture());
        assertNull(saved.getValue().getId());
        assertSame(alice, saved.getValue().getUser());
        assertEquals("Payee", saved.getValue().getName());
        assertEquals("999", saved.getValue().getAccountNumber());
        verify(recipientDao, never()).findByIdAndUserUsername(any(), any());
    }

    @Test
    public void ownRecipientIsUpdatedInPlace() {
        Recipient existing = new Recipient();
        existing.setId(7L);
        existing.setUser(alice);
        existing.setAccountNumber("111");
        when(recipientDao.findByIdAndUserUsername(7L, "alice")).thenReturn(existing);
        when(recipientDao.save(existing)).thenReturn(existing);

        Recipient result = transactionService.saveRecipient(form(7L), alice);

        assertSame(existing, result);
        assertEquals(Long.valueOf(7L), existing.getId());
        assertSame(alice, existing.getUser());
        assertEquals("999", existing.getAccountNumber());
        assertEquals("rent", existing.getDescription());
    }

    @Test(expected = RecipientNotFoundException.class)
    public void otherUsersRecipientIdIsRejected() {
        when(recipientDao.findByIdAndUserUsername(7L, "bob")).thenReturn(null);

        try {
            transactionService.saveRecipient(form(7L), bob);
        } finally {
            verify(recipientDao, never()).save(any(Recipient.class));
        }
    }

    @Test
    public void recipientLookupByNameIsScopedToCaller() {
        Recipient own = new Recipient();
        when(recipientDao.findFirstByNameAndUserUsername("Landlord", "alice")).thenReturn(own);
        Principal alicePrincipal = () -> "alice";

        assertSame(own, transactionService.findRecipientByName("Landlord", alicePrincipal));
    }

    @Test(expected = RecipientNotFoundException.class)
    public void otherUsersRecipientNameIsRejected() {
        when(recipientDao.findFirstByNameAndUserUsername("Landlord", "bob")).thenReturn(null);
        Principal bobPrincipal = () -> "bob";

        transactionService.findRecipientByName("Landlord", bobPrincipal);
    }

    @Test
    public void deleteByNameIsScopedToCaller() {
        Principal bobPrincipal = () -> "bob";

        transactionService.deleteRecipientByName("Landlord", bobPrincipal);

        verify(recipientDao).deleteByNameAndUserUsername("Landlord", "bob");
    }
}
