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
import com.userfront.validation.InvalidAmountException;

@RunWith(MockitoJUnitRunner.class)
public class TransactionServiceImplTest {

    private static final String HUGE_EXPONENT = "1E999999999";

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
        primaryAccount.setAccountBalance(new BigDecimal("500.00"));
        savingsAccount = new SavingsAccount();
        savingsAccount.setAccountBalance(new BigDecimal("200.00"));
    }

    @Test
    public void betweenAccountsTransferMovesValidAmount() throws Exception {
        transactionService.betweenAccountsTransfer("Primary", "Savings", "25.50", primaryAccount, savingsAccount);

        assertEquals(new BigDecimal("474.50"), primaryAccount.getAccountBalance());
        assertEquals(new BigDecimal("225.50"), savingsAccount.getAccountBalance());
        verify(primaryTransactionDao).save(any(PrimaryTransaction.class));
    }

    @Test(timeout = 5000)
    public void betweenAccountsTransferRejectsHugeExponentWithoutArithmetic() throws Exception {
        try {
            transactionService.betweenAccountsTransfer("Primary", "Savings", HUGE_EXPONENT, primaryAccount, savingsAccount);
            fail("Expected InvalidAmountException");
        } catch (InvalidAmountException expected) {
        }
        try {
            transactionService.betweenAccountsTransfer("Savings", "Primary", HUGE_EXPONENT, primaryAccount, savingsAccount);
            fail("Expected InvalidAmountException");
        } catch (InvalidAmountException expected) {
        }

        assertUnchanged();
    }

    @Test
    public void toSomeoneElseTransferDebitsValidAmount() {
        transactionService.toSomeoneElseTransfer(new Recipient(), "Savings", "20", primaryAccount, savingsAccount);

        assertEquals(new BigDecimal("180.00"), savingsAccount.getAccountBalance());
        verify(savingsTransactionDao).save(any(SavingsTransaction.class));
    }

    @Test(timeout = 5000)
    public void toSomeoneElseTransferRejectsHugeExponentWithoutArithmetic() {
        for (String accountType : new String[] {"Primary", "Savings"}) {
            try {
                transactionService.toSomeoneElseTransfer(new Recipient(), accountType, HUGE_EXPONENT, primaryAccount, savingsAccount);
                fail("Expected InvalidAmountException");
            } catch (InvalidAmountException expected) {
            }
        }

        assertUnchanged();
    }

    private void assertUnchanged() {
        assertEquals(new BigDecimal("500.00"), primaryAccount.getAccountBalance());
        assertEquals(new BigDecimal("200.00"), savingsAccount.getAccountBalance());
        verify(primaryAccountDao, never()).save(any(PrimaryAccount.class));
        verify(savingsAccountDao, never()).save(any(SavingsAccount.class));
        verify(primaryTransactionDao, never()).save(any(PrimaryTransaction.class));
        verify(savingsTransactionDao, never()).save(any(SavingsTransaction.class));
    }
}
