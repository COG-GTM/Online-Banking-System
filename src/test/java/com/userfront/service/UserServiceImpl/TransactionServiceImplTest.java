package com.userfront.service.UserServiceImpl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.springframework.test.util.ReflectionTestUtils;

import com.userfront.dao.PrimaryAccountDao;
import com.userfront.dao.PrimaryTransactionDao;
import com.userfront.dao.SavingsAccountDao;
import com.userfront.dao.SavingsTransactionDao;
import com.userfront.domain.PrimaryAccount;
import com.userfront.domain.PrimaryTransaction;
import com.userfront.domain.Recipient;
import com.userfront.domain.SavingsAccount;
import com.userfront.domain.SavingsTransaction;
import com.userfront.service.InsufficientFundsException;
import com.userfront.service.InvalidTransactionException;

public class TransactionServiceImplTest {

    private TransactionServiceImpl service;
    private PrimaryAccountDao primaryAccountDao;
    private SavingsAccountDao savingsAccountDao;
    private PrimaryTransactionDao primaryTransactionDao;
    private SavingsTransactionDao savingsTransactionDao;
    private PrimaryAccount primaryAccount;
    private SavingsAccount savingsAccount;
    private Recipient recipient;

    @Before
    public void setUp() {
        primaryAccountDao = mock(PrimaryAccountDao.class);
        savingsAccountDao = mock(SavingsAccountDao.class);
        primaryTransactionDao = mock(PrimaryTransactionDao.class);
        savingsTransactionDao = mock(SavingsTransactionDao.class);

        primaryAccount = new PrimaryAccount();
        primaryAccount.setId(1L);
        savingsAccount = new SavingsAccount();
        savingsAccount.setId(2L);
        recipient = new Recipient();
        recipient.setName("bob");

        service = new TransactionServiceImpl();
        ReflectionTestUtils.setField(service, "primaryAccountDao", primaryAccountDao);
        ReflectionTestUtils.setField(service, "savingsAccountDao", savingsAccountDao);
        ReflectionTestUtils.setField(service, "primaryTransactionDao", primaryTransactionDao);
        ReflectionTestUtils.setField(service, "savingsTransactionDao", savingsTransactionDao);
    }

    @Test
    public void negativeBetweenAccountsTransferIsRejectedBeforeTouchingBalances() {
        try {
            service.betweenAccountsTransfer("Primary", "Savings", new BigDecimal("-1000000"), primaryAccount, savingsAccount);
            fail("negative transfer accepted");
        } catch (InvalidTransactionException expected) {
        }
        verifyNoInteractions(primaryAccountDao, savingsAccountDao, primaryTransactionDao, savingsTransactionDao);
    }

    @Test
    public void overdraftBetweenAccountsTransferCreditsNothingAndRecordsNothing() {
        when(primaryAccountDao.findBalanceById(1L)).thenReturn(new BigDecimal("100.00"));

        try {
            service.betweenAccountsTransfer("Primary", "Savings", new BigDecimal("100.01"), primaryAccount, savingsAccount);
            fail("overdraft accepted");
        } catch (InsufficientFundsException expected) {
        }

        verify(primaryAccountDao, never()).compareAndSetBalance(anyLong(), any(BigDecimal.class), any(BigDecimal.class));
        verify(savingsAccountDao, never()).creditBalance(anyLong(), any(BigDecimal.class));
        verifyNoInteractions(primaryTransactionDao, savingsTransactionDao);
    }

    @Test
    public void coveredBetweenAccountsTransferDebitsThenCreditsAndRecordsSourceBalance() {
        when(savingsAccountDao.findBalanceById(2L)).thenReturn(new BigDecimal("50.00"));
        when(savingsAccountDao.compareAndSetBalance(2L, new BigDecimal("50.00"), new BigDecimal("29.75"))).thenReturn(1);

        service.betweenAccountsTransfer("Savings", "Primary", new BigDecimal("20.25"), primaryAccount, savingsAccount);

        InOrder lockOrder = inOrder(primaryAccountDao, savingsAccountDao);
        lockOrder.verify(primaryAccountDao).findBalanceById(1L);
        lockOrder.verify(savingsAccountDao).findBalanceById(2L);
        verify(primaryAccountDao).creditBalance(1L, new BigDecimal("20.25"));
        ArgumentCaptor<SavingsTransaction> tx = ArgumentCaptor.forClass(SavingsTransaction.class);
        verify(savingsTransactionDao).save(tx.capture());
        assertEquals(20.25, tx.getValue().getAmount(), 0.0);
        assertEquals(new BigDecimal("29.75"), tx.getValue().getAvailableBalance());
    }

    @Test
    public void sameAccountTransferIsRejected() {
        try {
            service.betweenAccountsTransfer("Primary", "Primary", new BigDecimal("5"), primaryAccount, savingsAccount);
            fail("same-account transfer accepted");
        } catch (InvalidTransactionException expected) {
        }
        verifyNoInteractions(primaryAccountDao, savingsAccountDao, primaryTransactionDao, savingsTransactionDao);
    }

    @Test
    public void overScaleAmountIsRejected() {
        try {
            service.toSomeoneElseTransfer(recipient, "Primary", new BigDecimal("0.001"), primaryAccount, savingsAccount);
            fail("sub-cent transfer accepted");
        } catch (InvalidTransactionException expected) {
        }
        verifyNoInteractions(primaryAccountDao, savingsAccountDao, primaryTransactionDao, savingsTransactionDao);
    }

    @Test
    public void negativeTransferToRecipientIsRejected() {
        try {
            service.toSomeoneElseTransfer(recipient, "Savings", new BigDecimal("-50"), primaryAccount, savingsAccount);
            fail("negative recipient transfer accepted");
        } catch (InvalidTransactionException expected) {
        }
        verifyNoInteractions(primaryAccountDao, savingsAccountDao, primaryTransactionDao, savingsTransactionDao);
    }

    @Test
    public void overdraftTransferToRecipientIsRejected() {
        when(savingsAccountDao.findBalanceById(2L)).thenReturn(new BigDecimal("10.00"));

        try {
            service.toSomeoneElseTransfer(recipient, "Savings", new BigDecimal("10.01"), primaryAccount, savingsAccount);
            fail("overdraft accepted");
        } catch (InsufficientFundsException expected) {
        }

        verify(savingsAccountDao, never()).compareAndSetBalance(anyLong(), any(BigDecimal.class), any(BigDecimal.class));
        verifyNoInteractions(primaryTransactionDao, savingsTransactionDao);
    }

    @Test
    public void transferToRecipientRechecksFundsAfterLosingARace() {
        when(primaryAccountDao.findBalanceById(1L))
            .thenReturn(new BigDecimal("100.00"))
            .thenReturn(new BigDecimal("30.00"));
        when(primaryAccountDao.compareAndSetBalance(1L, new BigDecimal("100.00"), new BigDecimal("40.00"))).thenReturn(0);

        try {
            service.toSomeoneElseTransfer(recipient, "Primary", new BigDecimal("60"), primaryAccount, savingsAccount);
            fail("overdraft accepted after concurrent debit");
        } catch (InsufficientFundsException expected) {
        }

        verify(primaryTransactionDao, never()).save(any(PrimaryTransaction.class));
    }

    @Test
    public void unknownRecipientIsRejected() {
        try {
            service.toSomeoneElseTransfer(null, "Primary", new BigDecimal("5"), primaryAccount, savingsAccount);
            fail("missing recipient accepted");
        } catch (InvalidTransactionException expected) {
        }
        verifyNoInteractions(primaryAccountDao, savingsAccountDao, primaryTransactionDao, savingsTransactionDao);
    }
}
