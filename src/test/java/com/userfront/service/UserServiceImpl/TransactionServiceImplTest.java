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
import com.userfront.dao.RecipientDao;
import com.userfront.dao.SavingsAccountDao;
import com.userfront.dao.SavingsTransactionDao;
import com.userfront.domain.PrimaryAccount;
import com.userfront.domain.PrimaryTransaction;
import com.userfront.domain.Recipient;
import com.userfront.domain.SavingsAccount;
import com.userfront.service.UserService;
import com.userfront.validation.InvalidAmountException;

@RunWith(MockitoJUnitRunner.class)
public class TransactionServiceImplTest {

    @Mock
    private UserService userService;
    @Mock
    private PrimaryTransactionDao primaryTransactionDao;
    @Mock
    private SavingsTransactionDao savingsTransactionDao;
    @Mock
    private PrimaryAccountDao primaryAccountDao;
    @Mock
    private SavingsAccountDao savingsAccountDao;
    @Mock
    private RecipientDao recipientDao;

    @InjectMocks
    private TransactionServiceImpl transactionService;

    private PrimaryAccount primaryAccount;
    private SavingsAccount savingsAccount;

    @Before
    public void setUp() {
        primaryAccount = new PrimaryAccount();
        primaryAccount.setAccountBalance(new BigDecimal("100.00"));
        savingsAccount = new SavingsAccount();
        savingsAccount.setAccountBalance(new BigDecimal("100.00"));
    }

    @Test
    public void betweenAccountsTransferMovesPositiveAmount() throws Exception {
        transactionService.betweenAccountsTransfer("Primary", "Savings", new BigDecimal("25.50"), primaryAccount, savingsAccount);

        assertEquals(new BigDecimal("74.50"), primaryAccount.getAccountBalance());
        assertEquals(new BigDecimal("125.50"), savingsAccount.getAccountBalance());
        verify(primaryTransactionDao).save(any(PrimaryTransaction.class));
    }

    @Test
    public void betweenAccountsTransferRejectsNegativeAmountWithoutTouchingBalances() throws Exception {
        try {
            transactionService.betweenAccountsTransfer("Primary", "Savings", new BigDecimal("-1000"), primaryAccount, savingsAccount);
            fail("Expected InvalidAmountException");
        } catch (InvalidAmountException expected) {
            // expected
        }

        assertBalancesUnchanged();
    }

    @Test
    public void betweenAccountsTransferRejectsZeroAndOverPreciseAmounts() throws Exception {
        for (BigDecimal amount : new BigDecimal[] {BigDecimal.ZERO, new BigDecimal("0.001")}) {
            try {
                transactionService.betweenAccountsTransfer("Savings", "Primary", amount, primaryAccount, savingsAccount);
                fail("Expected InvalidAmountException for " + amount);
            } catch (InvalidAmountException expected) {
                // expected
            }
        }

        assertBalancesUnchanged();
    }

    @Test
    public void toSomeoneElseTransferRejectsNegativeAmount() {
        Recipient recipient = new Recipient();
        recipient.setName("mallory");

        try {
            transactionService.toSomeoneElseTransfer(recipient, "Primary", new BigDecimal("-50"), primaryAccount, savingsAccount);
            fail("Expected InvalidAmountException");
        } catch (InvalidAmountException expected) {
            // expected
        }

        assertBalancesUnchanged();
    }

    private void assertBalancesUnchanged() {
        assertEquals(new BigDecimal("100.00"), primaryAccount.getAccountBalance());
        assertEquals(new BigDecimal("100.00"), savingsAccount.getAccountBalance());
        verify(primaryAccountDao, never()).save(any(PrimaryAccount.class));
        verify(savingsAccountDao, never()).save(any(SavingsAccount.class));
        verify(primaryTransactionDao, never()).save(any(PrimaryTransaction.class));
    }
}
