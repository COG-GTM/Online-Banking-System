package com.userfront.service.UserServiceImpl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.security.Principal;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.userfront.dao.PrimaryAccountDao;
import com.userfront.dao.SavingsAccountDao;
import com.userfront.domain.PrimaryAccount;
import com.userfront.domain.PrimaryTransaction;
import com.userfront.domain.SavingsAccount;
import com.userfront.domain.SavingsTransaction;
import com.userfront.domain.User;
import com.userfront.service.InsufficientFundsException;
import com.userfront.service.TransactionService;
import com.userfront.service.UserService;

@RunWith(MockitoJUnitRunner.class)
public class AccountServiceImplTest {

    @Mock
    private PrimaryAccountDao primaryAccountDao;

    @Mock
    private SavingsAccountDao savingsAccountDao;

    @Mock
    private UserService userService;

    @Mock
    private TransactionService transactionService;

    @InjectMocks
    private AccountServiceImpl accountService;

    private final Principal principal = () -> "alice";
    private PrimaryAccount primaryAccount;
    private SavingsAccount savingsAccount;

    @Before
    public void setUp() {
        primaryAccount = new PrimaryAccount();
        primaryAccount.setAccountBalance(new BigDecimal("100.00"));
        savingsAccount = new SavingsAccount();
        savingsAccount.setAccountBalance(new BigDecimal("50.00"));
        User user = new User();
        user.setPrimaryAccount(primaryAccount);
        user.setSavingsAccount(savingsAccount);
        when(userService.findByUsername("alice")).thenReturn(user);
    }

    @Test
    public void withdrawWithinBalanceDebitsAccount() {
        accountService.withdraw("Primary", new BigDecimal("40.00"), principal);

        assertEquals(new BigDecimal("60.00"), primaryAccount.getAccountBalance());
        verify(primaryAccountDao).save(primaryAccount);
        ArgumentCaptor<PrimaryTransaction> tx = ArgumentCaptor.forClass(PrimaryTransaction.class);
        verify(transactionService).savePrimaryWithdrawTransaction(tx.capture());
        assertEquals("Finished", tx.getValue().getStatus());
    }

    @Test
    public void withdrawOfEntireBalanceIsAllowed() {
        accountService.withdraw("Savings", new BigDecimal("50.00"), principal);

        assertEquals(0, savingsAccount.getAccountBalance().signum());
    }

    @Test
    public void primaryWithdrawBeyondBalanceIsDeclined() {
        try {
            accountService.withdraw("Primary", new BigDecimal("100.01"), principal);
            fail("expected InsufficientFundsException");
        } catch (InsufficientFundsException expected) {
            // expected
        }

        assertEquals(new BigDecimal("100.00"), primaryAccount.getAccountBalance());
        verify(primaryAccountDao, never()).save(any(PrimaryAccount.class));
        ArgumentCaptor<PrimaryTransaction> tx = ArgumentCaptor.forClass(PrimaryTransaction.class);
        verify(transactionService).savePrimaryWithdrawTransaction(tx.capture());
        assertEquals("Declined", tx.getValue().getStatus());
        assertEquals(new BigDecimal("100.00"), tx.getValue().getAvailableBalance());
    }

    @Test
    public void savingsWithdrawBeyondBalanceIsDeclined() {
        try {
            accountService.withdraw("Savings", new BigDecimal("1000"), principal);
            fail("expected InsufficientFundsException");
        } catch (InsufficientFundsException expected) {
            // expected
        }

        assertEquals(new BigDecimal("50.00"), savingsAccount.getAccountBalance());
        verify(savingsAccountDao, never()).save(any(SavingsAccount.class));
        ArgumentCaptor<SavingsTransaction> tx = ArgumentCaptor.forClass(SavingsTransaction.class);
        verify(transactionService).saveSavingsWithdrawTransaction(tx.capture());
        assertEquals("Declined", tx.getValue().getStatus());
    }

    @Test
    public void negativeWithdrawIsRejected() {
        try {
            accountService.withdraw("Primary", new BigDecimal("-500"), principal);
            fail("expected IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
            // expected
        }

        assertEquals(new BigDecimal("100.00"), primaryAccount.getAccountBalance());
    }

    @Test
    public void negativeDepositIsRejected() {
        try {
            accountService.deposit("Primary", new BigDecimal("-500"), principal);
            fail("expected IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
            // expected
        }

        assertEquals(new BigDecimal("100.00"), primaryAccount.getAccountBalance());
        verify(primaryAccountDao, never()).save(any(PrimaryAccount.class));
    }
}
