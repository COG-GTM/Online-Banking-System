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
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.userfront.dao.RecipientDao;
import com.userfront.domain.Recipient;
import com.userfront.domain.User;
import com.userfront.service.UserService;

@RunWith(MockitoJUnitRunner.class)
public class TransactionServiceImplRecipientTest {

    @Mock
    private UserService userService;

    @Mock
    private RecipientDao recipientDao;

    @InjectMocks
    private TransactionServiceImpl transactionService;

    private User alice;
    private User mallory;

    @Before
    public void setUp() {
        alice = new User();
        alice.setUsername("alice");
        mallory = new User();
        mallory.setUsername("mallory");
        when(userService.findByUsername("alice")).thenReturn(alice);
        when(userService.findByUsername("mallory")).thenReturn(mallory);
    }

    @Test
    public void findRecipientByNameIsScopedToCaller() {
        Recipient landlord = recipient(12L, "Landlord", alice);
        when(recipientDao.findFirstByNameAndUser("Landlord", alice)).thenReturn(landlord);

        assertSame(landlord, transactionService.findRecipientByName("Landlord", "alice"));
        assertNotFound(() -> transactionService.findRecipientByName("Landlord", "mallory"));
    }

    @Test
    public void deleteRecipientByNameIsScopedToCaller() {
        when(recipientDao.deleteByNameAndUser("Landlord", alice)).thenReturn(1L);
        when(recipientDao.deleteByNameAndUser("Landlord", mallory)).thenReturn(0L);

        transactionService.deleteRecipientByName("Landlord", "alice");
        assertNotFound(() -> transactionService.deleteRecipientByName("Landlord", "mallory"));
    }

    @Test
    public void saveWithForeignIdIsRejected() {
        Recipient form = recipient(12L, "Hijacked", null);

        assertNotFound(() -> transactionService.saveRecipient(form, "mallory"));
        verify(recipientDao, never()).save(any(Recipient.class));
    }

    @Test
    public void saveWithOwnIdUpdatesExistingRecordOnly() {
        Recipient existing = recipient(12L, "Landlord", alice);
        when(recipientDao.findByIdAndUser(12L, alice)).thenReturn(existing);
        when(recipientDao.save(any(Recipient.class))).thenAnswer(i -> i.getArgument(0));

        Recipient form = recipient(12L, "Landlord LLC", mallory);
        Recipient saved = transactionService.saveRecipient(form, "alice");

        assertSame(existing, saved);
        assertEquals("Landlord LLC", saved.getName());
        assertSame(alice, saved.getUser());
    }

    @Test
    public void saveWithoutIdCreatesRecipientOwnedByCaller() {
        when(recipientDao.save(any(Recipient.class))).thenAnswer(i -> i.getArgument(0));

        Recipient form = recipient(null, "Plumber", alice);
        transactionService.saveRecipient(form, "mallory");

        ArgumentCaptor<Recipient> saved = ArgumentCaptor.forClass(Recipient.class);
        verify(recipientDao).save(saved.capture());
        assertNull(saved.getValue().getId());
        assertSame(mallory, saved.getValue().getUser());
        assertEquals("Plumber", saved.getValue().getName());
    }

    private static Recipient recipient(Long id, String name, User user) {
        Recipient recipient = new Recipient();
        recipient.setId(id);
        recipient.setName(name);
        recipient.setUser(user);
        return recipient;
    }

    private static void assertNotFound(Runnable action) {
        try {
            action.run();
            fail("Expected 404");
        } catch (ResponseStatusException e) {
            assertEquals(HttpStatus.NOT_FOUND, e.getStatus());
        }
    }
}
