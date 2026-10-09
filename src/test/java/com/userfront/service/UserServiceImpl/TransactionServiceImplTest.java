package com.userfront.service.UserServiceImpl;

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
import com.userfront.domain.Recipient;
import com.userfront.domain.SavingsAccount;
import com.userfront.domain.SavingsTransaction;
import com.userfront.service.InvalidTransferException;

@RunWith(MockitoJUnitRunner.class)
public class TransactionServiceImplTest {

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
        primaryAccount.setAccountBalance(new BigDecimal("100.00"));
        savingsAccount = new SavingsAccount();
        savingsAccount.setAccountBalance(new BigDecimal("50.00"));
        recipient = new Recipient();
        recipient.setName("bob");
    }

    @Test
    public void toSomeoneElseRejectsNegativeAmount() {
        assertRejected(() -> transactionService.toSomeoneElseTransfer(recipient, "Primary", "-1000000", primaryAccount, savingsAccount));
        assertBalancesUnchanged();
    }

    @Test
    public void toSomeoneElseRejectsZeroNonNumericBlankAndExcessScale() {
        for (String amount : new String[] {"0", "0.00", "abc", "", "   ", null, "1.001", "1,000", "-0.01", "NaN", "123456789012345678"}) {
            assertRejected(() -> transactionService.toSomeoneElseTransfer(recipient, "Savings", amount, primaryAccount, savingsAccount));
        }
        assertBalancesUnchanged();
    }

    @Test
    public void toSomeoneElseRejectsOverdraft() {
        assertRejected(() -> transactionService.toSomeoneElseTransfer(recipient, "Primary", "100.01", primaryAccount, savingsAccount));
        assertRejected(() -> transactionService.toSomeoneElseTransfer(recipient, "Savings", "50.01", primaryAccount, savingsAccount));
        assertBalancesUnchanged();
    }

    @Test
    public void toSomeoneElseRejectsUnknownRecipient() {
        assertRejected(() -> transactionService.toSomeoneElseTransfer(null, "Primary", "10", primaryAccount, savingsAccount));
        assertBalancesUnchanged();
    }

    @Test
    public void toSomeoneElseDebitsValidAmount() {
        transactionService.toSomeoneElseTransfer(recipient, "Primary", "100.00", primaryAccount, savingsAccount);

        assertEquals(0, primaryAccount.getAccountBalance().compareTo(BigDecimal.ZERO));
        verify(primaryAccountDao).save(primaryAccount);
        verify(primaryTransactionDao).save(any(PrimaryTransaction.class));
    }

    @Test
    public void betweenAccountsRejectsNegativeAmount() {
        assertRejected(() -> transactionService.betweenAccountsTransfer("Primary", "Savings", "-500", primaryAccount, savingsAccount));
        assertRejected(() -> transactionService.betweenAccountsTransfer("Savings", "Primary", "-500", primaryAccount, savingsAccount));
        assertBalancesUnchanged();
    }

    @Test
    public void betweenAccountsRejectsOverdraft() {
        assertRejected(() -> transactionService.betweenAccountsTransfer("Primary", "Savings", "100.01", primaryAccount, savingsAccount));
        assertRejected(() -> transactionService.betweenAccountsTransfer("Savings", "Primary", "50.01", primaryAccount, savingsAccount));
        assertBalancesUnchanged();
    }

    @Test
    public void betweenAccountsMovesValidAmount() throws Exception {
        transactionService.betweenAccountsTransfer("Savings", "Primary", "50", primaryAccount, savingsAccount);

        assertEquals(0, primaryAccount.getAccountBalance().compareTo(new BigDecimal("150.00")));
        assertEquals(0, savingsAccount.getAccountBalance().compareTo(BigDecimal.ZERO));
        verify(savingsTransactionDao).save(any(SavingsTransaction.class));
    }

    private void assertBalancesUnchanged() {
        assertEquals(new BigDecimal("100.00"), primaryAccount.getAccountBalance());
        assertEquals(new BigDecimal("50.00"), savingsAccount.getAccountBalance());
        verify(primaryAccountDao, never()).save(any());
        verify(savingsAccountDao, never()).save(any());
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
