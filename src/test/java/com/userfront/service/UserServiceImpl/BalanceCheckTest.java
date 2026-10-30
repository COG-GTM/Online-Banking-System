package com.userfront.service.UserServiceImpl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.security.Principal;
import java.util.Optional;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
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
import com.userfront.domain.User;
import com.userfront.service.InsufficientFundsException;
import com.userfront.service.TransactionService;
import com.userfront.service.UserService;

public class BalanceCheckTest {

    private PrimaryAccountDao primaryAccountDao;
    private SavingsAccountDao savingsAccountDao;
    private PrimaryTransactionDao primaryTransactionDao;
    private SavingsTransactionDao savingsTransactionDao;
    private TransactionService transactionService;
    private AccountServiceImpl accountService;
    private TransactionServiceImpl transactionServiceImpl;
    private PrimaryAccount primary;
    private SavingsAccount savings;
    private Principal principal = () -> "alice";

    @Before
    public void setUp() {
        primaryAccountDao = mock(PrimaryAccountDao.class);
        savingsAccountDao = mock(SavingsAccountDao.class);
        primaryTransactionDao = mock(PrimaryTransactionDao.class);
        savingsTransactionDao = mock(SavingsTransactionDao.class);
        transactionService = mock(TransactionService.class);
        UserService userService = mock(UserService.class);

        primary = new PrimaryAccount();
        primary.setId(1L);
        primary.setAccountBalance(new BigDecimal("50.00"));
        savings = new SavingsAccount();
        savings.setId(2L);
        savings.setAccountBalance(new BigDecimal("20.00"));
        User user = new User();
        user.setPrimaryAccount(primary);
        user.setSavingsAccount(savings);
        when(userService.findByUsername("alice")).thenReturn(user);
        when(primaryAccountDao.findById(1L)).thenReturn(Optional.of(primary));
        when(savingsAccountDao.findById(2L)).thenReturn(Optional.of(savings));

        AccountLedger ledger = new AccountLedger();
        ReflectionTestUtils.setField(ledger, "primaryAccountDao", primaryAccountDao);
        ReflectionTestUtils.setField(ledger, "savingsAccountDao", savingsAccountDao);

        accountService = new AccountServiceImpl();
        ReflectionTestUtils.setField(accountService, "userService", userService);
        ReflectionTestUtils.setField(accountService, "transactionService", transactionService);
        ReflectionTestUtils.setField(accountService, "accountLedger", ledger);

        transactionServiceImpl = new TransactionServiceImpl();
        ReflectionTestUtils.setField(transactionServiceImpl, "primaryTransactionDao", primaryTransactionDao);
        ReflectionTestUtils.setField(transactionServiceImpl, "savingsTransactionDao", savingsTransactionDao);
        ReflectionTestUtils.setField(transactionServiceImpl, "accountLedger", ledger);
    }

    @Test
    public void withdrawBeyondBalanceIsDeclinedAndRecorded() {
        when(primaryAccountDao.debitIfSufficient(1L, new BigDecimal("75.00"))).thenReturn(0);

        try {
            accountService.withdraw("Primary", 75, principal);
            fail("expected InsufficientFundsException");
        } catch (InsufficientFundsException expected) {
        }

        ArgumentCaptor<PrimaryTransaction> tx = ArgumentCaptor.forClass(PrimaryTransaction.class);
        verify(transactionService).savePrimaryWithdrawTransaction(tx.capture());
        assertEquals("Declined", tx.getValue().getStatus());
        assertEquals(new BigDecimal("50.00"), tx.getValue().getAvailableBalance());
    }

    @Test
    public void withdrawWithinBalanceIsFinished() {
        when(savingsAccountDao.debitIfSufficient(2L, new BigDecimal("20.00"))).thenReturn(1);

        accountService.withdraw("Savings", 20, principal);

        ArgumentCaptor<SavingsTransaction> tx = ArgumentCaptor.forClass(SavingsTransaction.class);
        verify(transactionService).saveSavingsWithdrawTransaction(tx.capture());
        assertEquals("Finished", tx.getValue().getStatus());
    }

    @Test
    public void transferToRecipientBeyondBalanceIsDeclined() {
        when(savingsAccountDao.debitIfSufficient(2L, new BigDecimal("20.01"))).thenReturn(0);
        Recipient recipient = new Recipient();
        recipient.setName("bob");

        try {
            transactionServiceImpl.toSomeoneElseTransfer(recipient, "Savings", "20.01", primary, savings);
            fail("expected InsufficientFundsException");
        } catch (InsufficientFundsException expected) {
        }

        ArgumentCaptor<SavingsTransaction> tx = ArgumentCaptor.forClass(SavingsTransaction.class);
        verify(savingsTransactionDao).save(tx.capture());
        assertEquals("Declined", tx.getValue().getStatus());
    }

    @Test
    public void betweenAccountsTransferDoesNotCreditWhenDebitFails() throws Exception {
        when(primaryAccountDao.debitIfSufficient(1L, new BigDecimal("50.01"))).thenReturn(0);

        try {
            transactionServiceImpl.betweenAccountsTransfer("Primary", "Savings", "50.01", primary, savings);
            fail("expected InsufficientFundsException");
        } catch (InsufficientFundsException expected) {
        }

        verify(savingsAccountDao, never()).credit(any(), any());
        ArgumentCaptor<PrimaryTransaction> tx = ArgumentCaptor.forClass(PrimaryTransaction.class);
        verify(primaryTransactionDao).save(tx.capture());
        assertEquals("Declined", tx.getValue().getStatus());
    }

    @Test
    public void betweenAccountsTransferCreditsAfterSuccessfulDebit() throws Exception {
        when(savingsAccountDao.debitIfSufficient(2L, new BigDecimal("5.00"))).thenReturn(1);
        when(primaryAccountDao.credit(1L, new BigDecimal("5.00"))).thenReturn(1);

        transactionServiceImpl.betweenAccountsTransfer("Savings", "Primary", "5", primary, savings);

        verify(primaryAccountDao).credit(eq(1L), eq(new BigDecimal("5.00")));
    }
}
