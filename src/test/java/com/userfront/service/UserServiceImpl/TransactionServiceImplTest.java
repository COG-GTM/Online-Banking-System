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
import org.mockito.ArgumentCaptor;
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
import com.userfront.domain.SavingsTransaction;
import com.userfront.service.InsufficientFundsException;
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
    private PrimaryAccountDao primaryAccountDao;

    @Mock
    private SavingsAccountDao savingsAccountDao;

    @Mock
    private RecipientDao recipientDao;

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
        recipient.setName("Bob");
    }

    @Test
    public void toSomeoneElseWithinBalanceDebitsAccount() {
        transactionService.toSomeoneElseTransfer(recipient, "Primary", "25.00", primaryAccount, savingsAccount);

        assertEquals(new BigDecimal("75.00"), primaryAccount.getAccountBalance());
        verify(primaryAccountDao).save(primaryAccount);
    }

    @Test
    public void toSomeoneElseBeyondBalanceIsDeclined() {
        try {
            transactionService.toSomeoneElseTransfer(recipient, "Savings", "50.01", primaryAccount, savingsAccount);
            fail("expected InsufficientFundsException");
        } catch (InsufficientFundsException expected) {
            // expected
        }

        assertEquals(new BigDecimal("50.00"), savingsAccount.getAccountBalance());
        verify(savingsAccountDao, never()).save(any(SavingsAccount.class));
        ArgumentCaptor<SavingsTransaction> tx = ArgumentCaptor.forClass(SavingsTransaction.class);
        verify(savingsTransactionDao).save(tx.capture());
        assertEquals("Declined", tx.getValue().getStatus());
    }

    @Test
    public void toSomeoneElseWithNegativeAmountIsRejected() {
        try {
            transactionService.toSomeoneElseTransfer(recipient, "Primary", "-1000", primaryAccount, savingsAccount);
            fail("expected IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
            // expected
        }

        assertEquals(new BigDecimal("100.00"), primaryAccount.getAccountBalance());
        verify(primaryAccountDao, never()).save(any(PrimaryAccount.class));
    }

    @Test(expected = IllegalArgumentException.class)
    public void toSomeoneElseWithUnknownRecipientIsRejected() {
        transactionService.toSomeoneElseTransfer(null, "Primary", "10", primaryAccount, savingsAccount);
    }

    @Test
    public void betweenAccountsBeyondBalanceIsDeclined() throws Exception {
        try {
            transactionService.betweenAccountsTransfer("Primary", "Savings", "150", primaryAccount, savingsAccount);
            fail("expected InsufficientFundsException");
        } catch (InsufficientFundsException expected) {
            // expected
        }

        assertEquals(new BigDecimal("100.00"), primaryAccount.getAccountBalance());
        assertEquals(new BigDecimal("50.00"), savingsAccount.getAccountBalance());
        verify(primaryAccountDao, never()).save(any(PrimaryAccount.class));
        verify(savingsAccountDao, never()).save(any(SavingsAccount.class));
        ArgumentCaptor<PrimaryTransaction> tx = ArgumentCaptor.forClass(PrimaryTransaction.class);
        verify(primaryTransactionDao).save(tx.capture());
        assertEquals("Declined", tx.getValue().getStatus());
    }

    @Test
    public void betweenAccountsWithinBalanceMovesFunds() throws Exception {
        transactionService.betweenAccountsTransfer("Savings", "Primary", "50", primaryAccount, savingsAccount);

        assertEquals(new BigDecimal("150.00"), primaryAccount.getAccountBalance());
        assertEquals(0, savingsAccount.getAccountBalance().signum());
    }
}
