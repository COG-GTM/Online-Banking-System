package com.userfront.service.UserServiceImpl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.userfront.dao.RecipientDao;
import com.userfront.domain.Recipient;
import com.userfront.domain.User;
import com.userfront.dto.RecipientForm;
import com.userfront.service.RecipientNotFoundException;

public class TransactionServiceImplSaveRecipientTest {

    @Mock
    private RecipientDao recipientDao;

    @InjectMocks
    private TransactionServiceImpl transactionService;

    private User alice;
    private User mallory;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        when(recipientDao.save(any(Recipient.class))).thenAnswer(invocation -> invocation.getArgument(0));
        alice = user("alice");
        mallory = user("mallory");
    }

    @Test
    public void newRecipientIsCreatedWithoutIdAndOwnedByCaller() {
        Recipient saved = transactionService.saveRecipient(form(null, "Landlord"), alice);

        assertNull(saved.getId());
        assertSame(alice, saved.getUser());
        assertEquals("Landlord", saved.getName());
        assertEquals("landlord@example.com", saved.getEmail());
        assertEquals("555-0100", saved.getPhone());
        assertEquals("12345", saved.getAccountNumber());
        assertEquals("rent", saved.getDescription());
    }

    @Test
    public void editOfOwnRecipientUpdatesTheLoadedRow() {
        Recipient existing = recipient(42L, "Old name", alice);
        when(recipientDao.findByIdAndUserUsername(42L, "alice")).thenReturn(existing);

        Recipient saved = transactionService.saveRecipient(form(42L, "Landlord"), alice);

        assertSame(existing, saved);
        assertEquals(Long.valueOf(42L), saved.getId());
        assertSame(alice, saved.getUser());
        assertEquals("Landlord", saved.getName());
    }

    @Test
    public void foreignRecipientIdIsRejectedAndNothingIsSaved() {
        when(recipientDao.findByIdAndUserUsername(42L, "mallory")).thenReturn(null);

        try {
            transactionService.saveRecipient(form(42L, "Mallory's"), mallory);
            fail("expected RecipientNotFoundException");
        } catch (RecipientNotFoundException expected) {
            // expected
        }

        verify(recipientDao).findByIdAndUserUsername(42L, "mallory");
        verify(recipientDao, never()).save(any(Recipient.class));
    }

    @Test
    public void savedEntityNeverCarriesAnIdOtherThanTheOwnedRow() {
        Recipient existing = recipient(7L, "Mine", alice);
        when(recipientDao.findByIdAndUserUsername(7L, "alice")).thenReturn(existing);

        transactionService.saveRecipient(form(7L, "Mine v2"), alice);

        ArgumentCaptor<Recipient> captor = ArgumentCaptor.forClass(Recipient.class);
        verify(recipientDao).save(captor.capture());
        assertSame(existing, captor.getValue());
        assertSame(alice, captor.getValue().getUser());
    }

    private static User user(String username) {
        User user = new User();
        user.setUsername(username);
        return user;
    }

    private static Recipient recipient(Long id, String name, User owner) {
        Recipient recipient = new Recipient();
        recipient.setId(id);
        recipient.setName(name);
        recipient.setUser(owner);
        return recipient;
    }

    private static RecipientForm form(Long id, String name) {
        RecipientForm form = new RecipientForm();
        form.setId(id);
        form.setName(name);
        form.setEmail("landlord@example.com");
        form.setPhone("555-0100");
        form.setAccountNumber("12345");
        form.setDescription("rent");
        return form;
    }
}
