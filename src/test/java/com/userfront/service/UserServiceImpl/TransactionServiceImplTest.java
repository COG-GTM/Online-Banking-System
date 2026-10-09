package com.userfront.service.UserServiceImpl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import com.userfront.domain.Recipient;
import com.userfront.domain.SavingsAccount;
import com.userfront.domain.SavingsTransaction;
import com.userfront.service.InvalidTransferException;

@RunWith(MockitoJUnitRunner.Silent.class)
public class TransactionServiceImplTest {

    private static final Long PRIMARY_ID = 1L;
    private static final Long SAVINGS_ID = 2L;

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
    private Recipient recipient;

    @Before
    public void setUp() {
        primaryAccount = new PrimaryAccount();
        primaryAccount.setId(PRIMARY_ID);
        primaryAccount.setAccountBalance(new BigDecimal("100.00"));
        savingsAccount = new SavingsAccount();
        savingsAccount.setId(SAVINGS_ID);
        savingsAccount.setAccountBalance(new BigDecimal("50.00"));
        recipient = new Recipient();
        recipient.setName("bob");
    }

    @Test
    public void toSomeoneElseRejectsNegativeAmount() {
        assertRejected(() -> transactionService.toSomeoneElseTransfer(recipient, "Primary", "-1000000", primaryAccount, savingsAccount));
        assertNoWrites();
    }

    @Test
    public void toSomeoneElseRejectsZeroNonNumericBlankAndExcessScale() {
        for (String amount : new String[] {"0", "0.00", "abc", "", "   ", null, "1.001", "1,000", "-0.01", "NaN", "123456789012345678"}) {
            assertRejected(() -> transactionService.toSomeoneElseTransfer(recipient, "Savings", amount, primaryAccount, savingsAccount));
        }
        assertNoWrites();
    }

    @Test
    public void toSomeoneElseRejectsWhenAtomicDebitFindsInsufficientFunds() {
        when(primaryAccountDao.debitIfSufficientFunds(PRIMARY_ID, new BigDecimal("100.01"))).thenReturn(0);

        assertRejected(() -> transactionService.toSomeoneElseTransfer(recipient, "Primary", "100.01", primaryAccount, savingsAccount));

        verify(primaryTransactionDao, never()).save(any());
        assertEquals(new BigDecimal("100.00"), primaryAccount.getAccountBalance());
    }

    @Test
    public void toSomeoneElseRejectsUnknownRecipientAndAccountType() {
        assertRejected(() -> transactionService.toSomeoneElseTransfer(null, "Primary", "10", primaryAccount, savingsAccount));
        assertRejected(() -> transactionService.toSomeoneElseTransfer(recipient, "Checking", "10", primaryAccount, savingsAccount));
        assertNoWrites();
    }

    @Test
    public void toSomeoneElseDebitsValidAmount() {
        when(primaryAccountDao.debitIfSufficientFunds(PRIMARY_ID, new BigDecimal("100.00"))).thenReturn(1);
        when(primaryAccountDao.findBalanceById(PRIMARY_ID)).thenReturn(new BigDecimal("0.00"));

        transactionService.toSomeoneElseTransfer(recipient, "Primary", "100.00", primaryAccount, savingsAccount);

        assertEquals(new BigDecimal("0.00"), primaryAccount.getAccountBalance());
        verify(primaryTransactionDao).save(any(PrimaryTransaction.class));
    }

    @Test
    public void betweenAccountsRejectsNegativeAmount() {
        assertRejected(() -> transactionService.betweenAccountsTransfer("Primary", "Savings", "-500", primaryAccount, savingsAccount));
        assertRejected(() -> transactionService.betweenAccountsTransfer("Savings", "Primary", "-500", primaryAccount, savingsAccount));
        assertNoWrites();
    }

    @Test
    public void betweenAccountsRejectsSameAccountWithInvalidTransferException() {
        assertRejected(() -> transactionService.betweenAccountsTransfer("Primary", "Primary", "10", primaryAccount, savingsAccount));
        assertNoWrites();
    }

    @Test
    public void betweenAccountsDoesNotCreditWhenDebitFails() {
        when(savingsAccountDao.debitIfSufficientFunds(SAVINGS_ID, new BigDecimal("50.01"))).thenReturn(0);

        assertRejected(() -> transactionService.betweenAccountsTransfer("Savings", "Primary", "50.01", primaryAccount, savingsAccount));

        verify(primaryAccountDao, never()).credit(anyLong(), any());
        verify(savingsTransactionDao, never()).save(any());
    }

    @Test
    public void betweenAccountsMovesValidAmount() throws Exception {
        when(savingsAccountDao.debitIfSufficientFunds(SAVINGS_ID, new BigDecimal("50"))).thenReturn(1);
        when(primaryAccountDao.findBalanceById(PRIMARY_ID)).thenReturn(new BigDecimal("150.00"));
        when(savingsAccountDao.findBalanceById(SAVINGS_ID)).thenReturn(new BigDecimal("0.00"));

        transactionService.betweenAccountsTransfer("Savings", "Primary", "50", primaryAccount, savingsAccount);

        verify(primaryAccountDao).credit(PRIMARY_ID, new BigDecimal("50"));
        assertEquals(new BigDecimal("150.00"), primaryAccount.getAccountBalance());
        assertEquals(new BigDecimal("0.00"), savingsAccount.getAccountBalance());
        verify(savingsTransactionDao).save(any(SavingsTransaction.class));
    }

    private void assertNoWrites() {
        assertEquals(new BigDecimal("100.00"), primaryAccount.getAccountBalance());
        assertEquals(new BigDecimal("50.00"), savingsAccount.getAccountBalance());
        verify(primaryAccountDao, never()).debitIfSufficientFunds(anyLong(), any());
        verify(savingsAccountDao, never()).debitIfSufficientFunds(anyLong(), any());
        verify(primaryAccountDao, never()).credit(anyLong(), any());
        verify(savingsAccountDao, never()).credit(anyLong(), any());
        verify(primaryTransactionDao, never()).save(any());
        verify(savingsTransactionDao, never()).save(any());
    }

    private interface Transfer {
        void run() throws Exception;
    }

    private static void assertRejected(Transfer transfer) {
        try {
            transfer.run();
            fail("Expected InvalidTransferException");
        } catch (InvalidTransferException expected) {
            // expected
        } catch (Exception e) {
            fail("Unexpected exception: " + e);
        }
    }
}
