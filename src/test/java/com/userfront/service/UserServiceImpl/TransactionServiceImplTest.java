package com.userfront.service.UserServiceImpl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
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
    private TransactionServiceImpl service;

    private PrimaryAccount primary;
    private SavingsAccount savings;
    private Recipient recipient;

    @Before
    public void setUp() {
        primary = new PrimaryAccount();
        primary.setAccountBalance(new BigDecimal("100.00"));
        savings = new SavingsAccount();
        savings.setAccountBalance(new BigDecimal("50.00"));
        recipient = new Recipient();
        recipient.setName("Bob");
    }

    @Test
    public void rejectsNegativeZeroMalformedAndOverPreciseAmounts() {
        String[] bad = {"-5", "-1000000", "0", "0.00", "", "  ", null, "abc", "1e400", "1E2", "1.005", "NaN", "Infinity", "+5", "1,000", "9999999999999"};
        for (String amount : bad) {
            expectRejected(() -> service.betweenAccountsTransfer("Primary", "Savings", amount, primary, savings));
            expectRejected(() -> service.toSomeoneElseTransfer(recipient, "Primary", amount, primary, savings));
        }
        assertBalancesUnchanged();
    }

    @Test
    public void rejectsTransfersThatWouldOverdrawTheSource() {
        expectRejected(() -> service.betweenAccountsTransfer("Primary", "Savings", "100.01", primary, savings));
        expectRejected(() -> service.betweenAccountsTransfer("Savings", "Primary", "50.01", primary, savings));
        expectRejected(() -> service.toSomeoneElseTransfer(recipient, "Savings", "1000000", primary, savings));
        assertBalancesUnchanged();
    }

    @Test
    public void rejectsUnknownRecipientAndAccountType() {
        expectRejected(() -> service.toSomeoneElseTransfer(null, "Primary", "5", primary, savings));
        expectRejected(() -> service.toSomeoneElseTransfer(recipient, "Checking", "5", primary, savings));
        expectRejected(() -> service.betweenAccountsTransfer("Primary", "Primary", "5", primary, savings));
        assertBalancesUnchanged();
    }

    @Test
    public void movesExactAmountBetweenAccounts() {
        service.betweenAccountsTransfer("Primary", "Savings", "100", primary, savings);
        assertEquals(new BigDecimal("0.00"), primary.getAccountBalance());
        assertEquals(new BigDecimal("150.00"), savings.getAccountBalance());

        service.betweenAccountsTransfer("Savings", "Primary", " 25.50 ", primary, savings);
        assertEquals(new BigDecimal("25.50"), primary.getAccountBalance());
        assertEquals(new BigDecimal("124.50"), savings.getAccountBalance());

        verify(primaryAccountDao, times(2)).save(primary);
        verify(savingsAccountDao, times(2)).save(savings);
        verify(primaryTransactionDao).save(any(PrimaryTransaction.class));
        verify(savingsTransactionDao).save(any(SavingsTransaction.class));
    }

    @Test
    public void payeeTransferDebitsSenderByPositiveAmount() {
        service.toSomeoneElseTransfer(recipient, "Savings", "12.34", primary, savings);
        assertEquals(new BigDecimal("37.66"), savings.getAccountBalance());
        assertEquals(new BigDecimal("100.00"), primary.getAccountBalance());
        verify(savingsTransactionDao).save(any(SavingsTransaction.class));
    }

    private void assertBalancesUnchanged() {
        assertEquals(new BigDecimal("100.00"), primary.getAccountBalance());
        assertEquals(new BigDecimal("50.00"), savings.getAccountBalance());
        verify(primaryAccountDao, never()).save(any(PrimaryAccount.class));
        verify(savingsAccountDao, never()).save(any(SavingsAccount.class));
        verify(primaryTransactionDao, never()).save(any(PrimaryTransaction.class));
        verify(savingsTransactionDao, never()).save(any(SavingsTransaction.class));
    }

    private static void expectRejected(Runnable transfer) {
        try {
            transfer.run();
            fail("expected InvalidTransferException");
        } catch (InvalidTransferException expected) {
            // expected
        }
    }
}
