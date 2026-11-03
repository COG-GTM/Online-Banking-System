package com.userfront.service.UserServiceImpl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.Arrays;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.transaction.annotation.Transactional;

import com.userfront.dao.PrimaryAccountDao;
import com.userfront.dao.PrimaryTransactionDao;
import com.userfront.dao.SavingsAccountDao;
import com.userfront.dao.SavingsTransactionDao;
import com.userfront.domain.PrimaryAccount;
import com.userfront.domain.PrimaryTransaction;
import com.userfront.domain.Recipient;
import com.userfront.domain.SavingsAccount;
import com.userfront.domain.SavingsTransaction;

@RunWith(MockitoJUnitRunner.Silent.class)
public class TransferAtomicityTest {

    @Mock private PrimaryAccountDao primaryAccountDao;
    @Mock private SavingsAccountDao savingsAccountDao;
    @Mock private PrimaryTransactionDao primaryTransactionDao;
    @Mock private SavingsTransactionDao savingsTransactionDao;
    @Mock private AccountLocks accountLocks;

    @InjectMocks private TransactionServiceImpl transactionService;

    private PrimaryAccount stalePrimary;
    private SavingsAccount staleSavings;
    private PrimaryAccount lockedPrimary;
    private SavingsAccount lockedSavings;
    private Recipient recipient;

    @Before
    public void setUp() {
        // The caller's copies are stale; the locked rows carry the latest committed balances.
        stalePrimary = primary(1L, "100");
        staleSavings = savings(2L, "100");
        lockedPrimary = primary(1L, "30");
        lockedSavings = savings(2L, "150");
        when(accountLocks.lockPrimary(1L)).thenReturn(lockedPrimary);
        when(accountLocks.lockSavings(2L)).thenReturn(lockedSavings);
        recipient = new Recipient();
        recipient.setName("bob");
    }

    @Test
    public void betweenAccountsTransferLocksPrimaryThenSavingsAndUsesLockedBalances() throws Exception {
        transactionService.betweenAccountsTransfer("Savings", "Primary", "50", stalePrimary, staleSavings);

        InOrder order = inOrder(accountLocks);
        order.verify(accountLocks).lockPrimary(1L);
        order.verify(accountLocks).lockSavings(2L);
        assertBalance("80", lockedPrimary);
        assertBalance("100", lockedSavings);
        assertBalance("100", stalePrimary);
        verify(primaryAccountDao).save(lockedPrimary);
        verify(savingsAccountDao).save(lockedSavings);
        verify(primaryAccountDao, never()).save(stalePrimary);
        verify(savingsTransactionDao).save(any(SavingsTransaction.class));
    }

    @Test
    public void betweenAccountsTransferLocksInSameOrderForBothDirections() throws Exception {
        transactionService.betweenAccountsTransfer("Primary", "Savings", "30", stalePrimary, staleSavings);

        InOrder order = inOrder(accountLocks);
        order.verify(accountLocks).lockPrimary(1L);
        order.verify(accountLocks).lockSavings(2L);
        assertBalance("0", lockedPrimary);
        assertBalance("180", lockedSavings);
        verify(primaryTransactionDao).save(any(PrimaryTransaction.class));
    }

    @Test
    public void betweenAccountsTransferRejectsAmountAboveLockedBalance() throws Exception {
        // The stale copy shows 100, but only 30 is left after a concurrent transfer committed.
        try {
            transactionService.betweenAccountsTransfer("Primary", "Savings", "50", stalePrimary, staleSavings);
            fail("expected TransferRejectedException");
        } catch (TransferRejectedException expected) {
            assertEquals("Insufficient funds for this transfer.", expected.getMessage());
        }
        assertBalance("30", lockedPrimary);
        assertBalance("150", lockedSavings);
        verifyNothingSaved();
    }

    @Test
    public void invalidTransferDirectionFailsBeforeLocking() {
        try {
            transactionService.betweenAccountsTransfer("Primary", "Primary", "50", stalePrimary, staleSavings);
            fail("expected Invalid Transfer");
        } catch (Exception expected) {
            assertEquals("Invalid Transfer", expected.getMessage());
        }
        verify(accountLocks, never()).lockPrimary(anyLong());
        verify(accountLocks, never()).lockSavings(anyLong());
        verifyNothingSaved();
    }

    @Test
    public void nonPositiveOrMalformedAmountsAreRejectedBeforeLocking() throws Exception {
        for (String amount : Arrays.asList("0", "-10", "abc", "", "1.005")) {
            try {
                transactionService.betweenAccountsTransfer("Savings", "Primary", amount, stalePrimary, staleSavings);
                fail("expected rejection for " + amount);
            } catch (TransferRejectedException expected) {
                // expected
            }
            try {
                transactionService.toSomeoneElseTransfer(recipient, "Savings", amount, stalePrimary, staleSavings);
                fail("expected rejection for " + amount);
            } catch (TransferRejectedException expected) {
                // expected
            }
        }
        verify(accountLocks, never()).lockPrimary(anyLong());
        verify(accountLocks, never()).lockSavings(anyLong());
        verifyNothingSaved();
    }

    @Test
    public void toSomeoneElseTransferDebitsLockedAccount() {
        transactionService.toSomeoneElseTransfer(recipient, "Savings", "40", stalePrimary, staleSavings);

        verify(accountLocks).lockSavings(2L);
        verify(accountLocks, never()).lockPrimary(anyLong());
        verify(savingsAccountDao).save(lockedSavings);
        assertBalance("110", lockedSavings);
        assertBalance("100", staleSavings);
    }

    @Test
    public void toSomeoneElseTransferRejectsAmountAboveLockedBalance() {
        try {
            transactionService.toSomeoneElseTransfer(recipient, "Primary", "40", stalePrimary, staleSavings);
            fail("expected TransferRejectedException");
        } catch (TransferRejectedException expected) {
            assertEquals("Insufficient funds for this transfer.", expected.getMessage());
        }
        assertBalance("30", lockedPrimary);
        verifyNothingSaved();
    }

    @Test
    public void toSomeoneElseTransferRejectsUnknownRecipientOrAccount() {
        for (Object[] args : new Object[][] {{null, "Primary"}, {recipient, "Checking"}}) {
            try {
                transactionService.toSomeoneElseTransfer((Recipient) args[0], (String) args[1], "10", stalePrimary, staleSavings);
                fail("expected TransferRejectedException");
            } catch (TransferRejectedException expected) {
                // expected
            }
        }
        verifyNothingSaved();
    }

    @Test
    public void transfersRunInSingleTransactionsThatRollBackOnAnyException() throws Exception {
        Transactional between = transactional(TransactionServiceImpl.class.getMethod("betweenAccountsTransfer",
                String.class, String.class, String.class, PrimaryAccount.class, SavingsAccount.class));
        assertTrue(Arrays.asList(between.rollbackFor()).contains(Exception.class));
        transactional(TransactionServiceImpl.class.getMethod("toSomeoneElseTransfer",
                Recipient.class, String.class, String.class, PrimaryAccount.class, SavingsAccount.class));
        assertTrue(RuntimeException.class.isAssignableFrom(TransferRejectedException.class));
    }

    @Test
    public void accountLocksRequireCallerTransaction() throws Exception {
        for (String name : Arrays.asList("lockPrimary", "lockSavings")) {
            Transactional tx = transactional(AccountLocks.class.getMethod(name, Long.class));
            assertEquals(org.springframework.transaction.annotation.Propagation.MANDATORY, tx.propagation());
        }
    }

    @Test
    public void accountsAreVersioned() throws Exception {
        assertNotNull(PrimaryAccount.class.getDeclaredField("version").getAnnotation(javax.persistence.Version.class));
        assertNotNull(SavingsAccount.class.getDeclaredField("version").getAnnotation(javax.persistence.Version.class));
    }

    private void verifyNothingSaved() {
        verify(primaryAccountDao, never()).save(any(PrimaryAccount.class));
        verify(savingsAccountDao, never()).save(any(SavingsAccount.class));
        verify(primaryTransactionDao, never()).save(any(PrimaryTransaction.class));
        verify(savingsTransactionDao, never()).save(any(SavingsTransaction.class));
    }

    private static Transactional transactional(Method method) {
        Transactional tx = method.getAnnotation(Transactional.class);
        assertNotNull(method.getName() + " must be @Transactional", tx);
        return tx;
    }

    private static void assertBalance(String expected, PrimaryAccount account) {
        assertEquals(0, new BigDecimal(expected).compareTo(account.getAccountBalance()));
    }

    private static void assertBalance(String expected, SavingsAccount account) {
        assertEquals(0, new BigDecimal(expected).compareTo(account.getAccountBalance()));
    }

    private static PrimaryAccount primary(Long id, String balance) {
        PrimaryAccount account = new PrimaryAccount();
        account.setId(id);
        account.setAccountBalance(new BigDecimal(balance));
        return account;
    }

    private static SavingsAccount savings(Long id, String balance) {
        SavingsAccount account = new SavingsAccount();
        account.setId(id);
        account.setAccountBalance(new BigDecimal(balance));
        return account;
    }
}
