package com.userfront.service.UserServiceImpl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import com.userfront.dao.PrimaryTransactionDao;
import com.userfront.domain.PrimaryAccount;
import com.userfront.domain.PrimaryTransaction;
import com.userfront.domain.User;
import com.userfront.service.TransactionService;
import com.userfront.service.UserService;

@RunWith(MockitoJUnitRunner.class)
public class TransactionServiceImplTest {

    @Mock
    private UserService userService;

    @Mock
    private PrimaryTransactionDao primaryTransactionDao;

    @InjectMocks
    private TransactionServiceImpl transactionService;

    private final PrimaryAccount primaryAccount = new PrimaryAccount();

    private void givenAliceOwnsThePrimaryAccount() {
        User user = new User();
        user.setPrimaryAccount(primaryAccount);
        when(userService.findByUsername("alice")).thenReturn(user);
    }

    @Test
    public void queriesTheRequestedPageOfTheAccountsLedger() {
        givenAliceOwnsThePrimaryAccount();
        Page<PrimaryTransaction> expected = new PageImpl<>(Collections.singletonList(new PrimaryTransaction()));
        when(primaryTransactionDao.findByPrimaryAccountOrderByDateDescIdDesc(eq(primaryAccount), any(Pageable.class))).thenReturn(expected);

        Page<PrimaryTransaction> result = transactionService.findPrimaryTransactionPage("alice", 2, 25);

        assertSame(expected, result);
        Pageable pageable = capturePageable();
        assertEquals(2, pageable.getPageNumber());
        assertEquals(25, pageable.getPageSize());
    }

    @Test
    public void capsThePageSize() {
        givenAliceOwnsThePrimaryAccount();
        transactionService.findPrimaryTransactionPage("alice", 0, 1_000_000);

        assertEquals(TransactionService.MAX_TRANSACTION_PAGE_SIZE, capturePageable().getPageSize());
    }

    @Test
    public void fallsBackToDefaultsForNonPositiveInput() {
        givenAliceOwnsThePrimaryAccount();
        transactionService.findPrimaryTransactionPage("alice", -3, 0);

        Pageable pageable = capturePageable();
        assertEquals(0, pageable.getPageNumber());
        assertEquals(TransactionService.DEFAULT_TRANSACTION_PAGE_SIZE, pageable.getPageSize());
    }

    @Test
    public void returnsAnEmptyPageForAnUnknownUser() {
        Page<PrimaryTransaction> result = transactionService.findPrimaryTransactionPage("nobody", 0, 10);

        assertTrue(result.getContent().isEmpty());
        assertEquals(0, result.getTotalElements());
        verify(primaryTransactionDao, never()).findByPrimaryAccountOrderByDateDescIdDesc(any(), any());
    }

    private Pageable capturePageable() {
        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(primaryTransactionDao).findByPrimaryAccountOrderByDateDescIdDesc(eq(primaryAccount), captor.capture());
        return captor.getValue();
    }
}
