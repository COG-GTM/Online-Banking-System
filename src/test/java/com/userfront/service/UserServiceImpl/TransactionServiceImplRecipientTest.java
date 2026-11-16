package com.userfront.service.UserServiceImpl;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.security.Principal;
import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import com.userfront.dao.RecipientDao;
import com.userfront.domain.Recipient;

public class TransactionServiceImplRecipientTest {

    private static final Principal ALICE = new UsernamePasswordAuthenticationToken("alice", "n/a");

    @Mock
    private RecipientDao recipientDao;

    @InjectMocks
    private TransactionServiceImpl service;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void recipientListIsQueriedByOwnerInsteadOfLoadingEveryRecipient() {
        List<Recipient> own = Collections.singletonList(recipient(1L, "Landlord"));
        when(recipientDao.findByUserUsername("alice")).thenReturn(own);

        assertSame(own, service.findRecipientList(ALICE));
        verify(recipientDao, never()).findAll();
    }

    @Test
    public void findAndDeleteAreScopedToPrincipal() {
        Recipient own = recipient(1L, "Landlord");
        when(recipientDao.findByIdAndUserUsername(1L, "alice")).thenReturn(own);

        assertSame(own, service.findRecipientById(1L, ALICE));
        service.deleteRecipientById(1L, ALICE);
        verify(recipientDao).deleteByIdAndUserUsername(1L, "alice");
    }

    @Test
    public void nameIsTakenOnlyByAnotherOfThePrincipalsRecipients() {
        when(recipientDao.findFirstByNameAndUserUsername("Landlord", "alice")).thenReturn(recipient(1L, "Landlord"));

        assertTrue(service.isRecipientNameTaken("Landlord", null, ALICE));
        assertTrue(service.isRecipientNameTaken("Landlord", 2L, ALICE));
        assertFalse(service.isRecipientNameTaken("Landlord", 1L, ALICE));
        assertFalse(service.isRecipientNameTaken("Mom", null, ALICE));
    }

    private static Recipient recipient(Long id, String name) {
        Recipient recipient = new Recipient();
        recipient.setId(id);
        recipient.setName(name);
        return recipient;
    }
}
