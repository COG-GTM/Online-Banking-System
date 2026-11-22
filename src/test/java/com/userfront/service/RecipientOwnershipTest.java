package com.userfront.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.security.Principal;
import java.util.Optional;

import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.access.AccessDeniedException;

import com.userfront.dao.RecipientDao;
import com.userfront.domain.Recipient;
import com.userfront.domain.User;
import com.userfront.service.UserServiceImpl.TransactionServiceImpl;

public class RecipientOwnershipTest {

    @Mock
    private UserService userService;

    @Mock
    private RecipientDao recipientDao;

    @InjectMocks
    private TransactionServiceImpl transactionService;

    private User alice;
    private User bob;
    private Principal alicePrincipal = () -> "alice";

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        alice = user(1L, "alice");
        bob = user(2L, "bob");
        when(userService.findByUsername("alice")).thenReturn(alice);
        when(userService.findByUsername("bob")).thenReturn(bob);
        when(recipientDao.save(any(Recipient.class))).thenAnswer(invocation -> invocation.getArguments()[0]);
    }

    @Test
    public void ownerJoinColumnIsMandatory() throws Exception {
        Field field = Recipient.class.getDeclaredField("user");
        assertFalse(field.getAnnotation(ManyToOne.class).optional());
        assertFalse(field.getAnnotation(JoinColumn.class).nullable());
        assertEquals("user_id", field.getAnnotation(JoinColumn.class).name());
    }

    @Test
    public void newRecipientIsOwnedByAuthenticatedUser() {
        Recipient recipient = recipient(null, null);

        Recipient saved = transactionService.saveRecipient(recipient, alicePrincipal);

        assertSame(alice, saved.getUser());
    }

    @Test
    public void submittedOwnerIsReplacedWithAuthenticatedUser() {
        Recipient recipient = recipient(null, bob);

        Recipient saved = transactionService.saveRecipient(recipient, alicePrincipal);

        assertSame(alice, saved.getUser());
    }

    @Test
    public void ownerCanEditOwnRecipient() {
        when(recipientDao.findById(10L)).thenReturn(Optional.of(recipient(10L, alice)));

        Recipient saved = transactionService.saveRecipient(recipient(10L, null), alicePrincipal);

        assertSame(alice, saved.getUser());
    }

    @Test
    public void editingAnotherUsersRecipientIsRejected() {
        when(recipientDao.findById(20L)).thenReturn(Optional.of(recipient(20L, bob)));

        assertRejected(recipient(20L, null), alicePrincipal);
    }

    @Test
    public void editingUnknownRecipientIdIsRejected() {
        when(recipientDao.findById(99L)).thenReturn(Optional.empty());

        assertRejected(recipient(99L, null), alicePrincipal);
    }

    @Test
    public void unresolvedOwnerIsRejected() {
        assertRejected(recipient(null, null), () -> "ghost");
        assertRejected(recipient(null, null), null);
    }

    private void assertRejected(Recipient recipient, Principal principal) {
        try {
            transactionService.saveRecipient(recipient, principal);
            fail("Expected AccessDeniedException");
        } catch (AccessDeniedException expected) {
            verify(recipientDao, never()).save(any(Recipient.class));
        }
    }

    private static User user(Long id, String username) {
        User user = new User();
        user.setUserId(id);
        user.setUsername(username);
        return user;
    }

    private static Recipient recipient(Long id, User owner) {
        Recipient recipient = new Recipient();
        recipient.setId(id);
        recipient.setName("payee-" + id);
        recipient.setUser(owner);
        return recipient;
    }
}
