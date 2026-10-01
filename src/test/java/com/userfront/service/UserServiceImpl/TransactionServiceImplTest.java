package com.userfront.service.UserServiceImpl;

import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.security.Principal;
import java.util.Collections;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.userfront.dao.RecipientDao;
import com.userfront.domain.Recipient;

@RunWith(MockitoJUnitRunner.class)
public class TransactionServiceImplTest {

    @Mock
    private RecipientDao recipientDao;

    @InjectMocks
    private TransactionServiceImpl transactionService;

    @Test
    public void findRecipientListQueriesOnlyThePrincipalsRecipients() {
        List<Recipient> recipients = Collections.singletonList(new Recipient());
        when(recipientDao.findByUserUsername("alice")).thenReturn(recipients);
        Principal principal = () -> "alice";

        assertSame(recipients, transactionService.findRecipientList(principal));
        verify(recipientDao, never()).findAll();
    }
}
