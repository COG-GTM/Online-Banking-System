package com.userfront.service.UserServiceImpl;

import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.Collections;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import com.userfront.dao.PrimaryTransactionDao;
import com.userfront.dao.SavingsTransactionDao;
import com.userfront.domain.PrimaryAccount;
import com.userfront.domain.PrimaryTransaction;
import com.userfront.domain.SavingsAccount;
import com.userfront.domain.SavingsTransaction;
import com.userfront.domain.User;
import com.userfront.service.TransactionPaging;
import com.userfront.service.UserService;

@RunWith(MockitoJUnitRunner.class)
public class TransactionServiceImplTest {

    @Mock
    private UserService userService;

    @Mock
    private PrimaryTransactionDao primaryTransactionDao;

    @Mock
    private SavingsTransactionDao savingsTransactionDao;

    @Mock
    private SavingsAccount savingsAccount;

    @Mock
    private PrimaryAccount primaryAccount;

    @InjectMocks
    private TransactionServiceImpl transactionService;

    @Before
    public void setUp() {
        User user = new User();
        user.setPrimaryAccount(primaryAccount);
        user.setSavingsAccount(savingsAccount);
        when(userService.findByUsername("alice")).thenReturn(user);
    }

    @Test
    public void savingsLedgerIsReadThroughBoundedQuery() {
        Pageable pageable = TransactionPaging.of(2, 25);
        Page<SavingsTransaction> page = new PageImpl<>(Collections.<SavingsTransaction>emptyList(), pageable, 0);
        when(savingsTransactionDao.findBySavingsAccountOrderByDateDescIdDesc(savingsAccount, pageable)).thenReturn(page);

        assertSame(page, transactionService.findSavingsTransactionPage("alice", pageable));

        verify(savingsTransactionDao).findBySavingsAccountOrderByDateDescIdDesc(savingsAccount, pageable);
        verifyNoMoreInteractions(savingsAccount);
    }

    @Test
    public void primaryLedgerIsReadThroughBoundedQuery() {
        Pageable pageable = TransactionPaging.of(0, 20);
        Page<PrimaryTransaction> page = new PageImpl<>(Collections.<PrimaryTransaction>emptyList(), pageable, 0);
        when(primaryTransactionDao.findByPrimaryAccountOrderByDateDescIdDesc(primaryAccount, pageable)).thenReturn(page);

        assertSame(page, transactionService.findPrimaryTransactionPage("alice", pageable));

        verify(primaryTransactionDao).findByPrimaryAccountOrderByDateDescIdDesc(primaryAccount, pageable);
        verifyNoMoreInteractions(primaryAccount);
    }
}
