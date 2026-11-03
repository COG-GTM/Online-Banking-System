package com.userfront.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.math.BigDecimal;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.userfront.dao.PrimaryAccountDao;
import com.userfront.dao.PrimaryTransactionDao;
import com.userfront.dao.SavingsAccountDao;
import com.userfront.dao.SavingsTransactionDao;
import com.userfront.domain.PrimaryAccount;
import com.userfront.domain.PrimaryTransaction;
import com.userfront.domain.SavingsAccount;
import com.userfront.domain.SavingsTransaction;
import com.userfront.service.UserServiceImpl.TransactionServiceImpl;

@RunWith(MockitoJUnitRunner.class)
public class BetweenAccountsTransferValidationTest {

    @Mock
    private PrimaryAccountDao primaryAccountDao;

    @Mock
    private SavingsAccountDao savingsAccountDao;

    @Mock
    private PrimaryTransactionDao primaryTransactionDao;

    @Mock
    private SavingsTransactionDao savingsTransactionDao;

    @InjectMocks
    private TransactionServiceImpl transactionService;

    private PrimaryAccount primaryAccount;
    private SavingsAccount savingsAccount;

    @Before
    public void setUp() {
        primaryAccount = new PrimaryAccount();
        primaryAccount.setAccountBalance(new BigDecimal("100.00"));
        savingsAccount = new SavingsAccount();
        savingsAccount.setAccountBalance(new BigDecimal("50.00"));
    }

    @Test
    public void validPrimaryToSavingsTransferMovesFunds() {
        transactionService.betweenAccountsTransfer("Primary", "Savings", "25.50", primaryAccount, savingsAccount);

        assertEquals(new BigDecimal("74.50"), primaryAccount.getAccountBalance());
        assertEquals(new BigDecimal("75.50"), savingsAccount.getAccountBalance());
        verify(primaryTransactionDao).save(any(PrimaryTransaction.class));
    }

    @Test
    public void validSavingsToPrimaryTransferMovesFunds() {
        transactionService.betweenAccountsTransfer("savings", "primary", " 50 ", primaryAccount, savingsAccount);

        assertEquals(new BigDecimal("150.00"), primaryAccount.getAccountBalance());
        assertEquals(new BigDecimal("0.00"), savingsAccount.getAccountBalance());
        verify(savingsTransactionDao).save(any(SavingsTransaction.class));
    }

    @Test
    public void rejectsSameAccount() {
        assertRejected("Primary", "Primary", "10");
        assertRejected("Savings", "savings", "10");
    }

    @Test
    public void rejectsUnknownAccounts() {
        assertRejected("Checking", "Savings", "10");
        assertRejected("", "", "10");
        assertRejected(null, null, "10");
    }

    @Test
    public void rejectsNegativeAndZeroAmounts() {
        assertRejected("Primary", "Savings", "-500");
        assertRejected("Primary", "Savings", "0");
        assertRejected("Primary", "Savings", "0.00");
    }

    @Test
    public void rejectsNonNumericAndMalformedAmounts() {
        assertRejected("Primary", "Savings", "");
        assertRejected("Primary", "Savings", null);
        assertRejected("Primary", "Savings", "abc");
        assertRejected("Primary", "Savings", "1e3");
        assertRejected("Primary", "Savings", "NaN");
        assertRejected("Primary", "Savings", "10.123");
        assertRejected("Primary", "Savings", "1,000");
        assertRejected("Primary", "Savings", "1234567890123456");
    }

    @Test
    public void rejectsAmountsAboveSourceBalance() {
        assertRejected("Primary", "Savings", "100.01");
        assertRejected("Savings", "Primary", "50.01");
    }

    private void assertRejected(String from, String to, String amount) {
        try {
            transactionService.betweenAccountsTransfer(from, to, amount, primaryAccount, savingsAccount);
            fail("Expected InvalidTransferException for " + from + " -> " + to + " amount " + amount);
        } catch (InvalidTransferException expected) {
            // expected
        }
        assertEquals(new BigDecimal("100.00"), primaryAccount.getAccountBalance());
        assertEquals(new BigDecimal("50.00"), savingsAccount.getAccountBalance());
        verify(primaryAccountDao, never()).save(any(PrimaryAccount.class));
        verify(savingsAccountDao, never()).save(any(SavingsAccount.class));
        verify(primaryTransactionDao, never()).save(any(PrimaryTransaction.class));
        verify(savingsTransactionDao, never()).save(any(SavingsTransaction.class));
    }
}
