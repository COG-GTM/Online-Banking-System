package com.userfront.service.UserServiceImpl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.security.Principal;
import java.util.Arrays;

import javax.persistence.EntityManager;
import javax.persistence.LockModeType;

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
import com.userfront.service.InvalidTransactionException;
import com.userfront.service.TransactionService;
import com.userfront.service.UserService;

@RunWith(MockitoJUnitRunner.Silent.class)
public class AccountServiceImplTest {

    @Mock
    private PrimaryAccountDao primaryAccountDao;

    @Mock
    private SavingsAccountDao savingsAccountDao;

    @Mock
    private UserService userService;

    @Mock
    private TransactionService transactionService;

    @Mock
    private EntityManager entityManager;

    @InjectMocks
    private AccountServiceImpl accountService;

    private final Principal principal = () -> "mallory";
    private PrimaryAccount primaryAccount;
    private SavingsAccount savingsAccount;

    @Before
    public void setUp() {
        primaryAccount = new PrimaryAccount();
        primaryAccount.setAccountBalance(new BigDecimal("10.00"));
        savingsAccount = new SavingsAccount();
        savingsAccount.setAccountBalance(new BigDecimal("10.00"));
        User user = new User();
        user.setPrimaryAccount(primaryAccount);
        user.setSavingsAccount(savingsAccount);
        when(userService.findByUsername("mallory")).thenReturn(user);
    }

    @Test
    public void depositCreditsTheLockedAccountAndRecordsTheAmount() {
        accountService.deposit("Primary", new BigDecimal("2.50"), principal);

        verify(entityManager).refresh(primaryAccount, LockModeType.PESSIMISTIC_WRITE);
        assertEquals(new BigDecimal("12.50"), primaryAccount.getAccountBalance());
        ArgumentCaptor<PrimaryTransaction> tx = ArgumentCaptor.forClass(PrimaryTransaction.class);
        verify(transactionService).savePrimaryDepositTransaction(tx.capture());
        assertEquals(2.50, tx.getValue().getAmount(), 0.0);
    }

    @Test
    public void withdrawDebitsUpToTheFullBalance() {
        accountService.withdraw("Savings", new BigDecimal("10.00"), principal);

        verify(entityManager).refresh(savingsAccount, LockModeType.PESSIMISTIC_WRITE);
        assertEquals(0, savingsAccount.getAccountBalance().signum());
        verify(savingsAccountDao).save(savingsAccount);
        verify(transactionService).saveSavingsWithdrawTransaction(any(SavingsTransaction.class));
    }

    @Test
    public void withdrawMoreThanTheBalanceIsRejectedAndNothingIsSaved() {
        for (String type : Arrays.asList("Primary", "Savings")) {
            try {
                accountService.withdraw(type, new BigDecimal("1000000"), principal);
                fail("expected insufficient funds for " + type);
            } catch (InvalidTransactionException expected) {
                // expected
            }
        }
        assertBalancesUnchangedAndNothingSaved();
    }

    @Test
    public void negativeZeroAndOverLimitAmountsAreRejectedForBothOperations() {
        BigDecimal[] bad = {new BigDecimal("-1000000"), new BigDecimal("-5"), BigDecimal.ZERO,
                new BigDecimal("0.001"), new BigDecimal("1000000.01")};
        for (BigDecimal amount : bad) {
            for (String type : Arrays.asList("Primary", "Savings")) {
                assertRejected(() -> accountService.deposit(type, amount, principal));
                assertRejected(() -> accountService.withdraw(type, amount, principal));
            }
        }
        assertBalancesUnchangedAndNothingSaved();
    }

    @Test
    public void unknownAccountTypeIsRejected() {
        assertRejected(() -> accountService.deposit("Checking", new BigDecimal("1"), principal));
        assertRejected(() -> accountService.withdraw(null, new BigDecimal("1"), principal));
        assertBalancesUnchangedAndNothingSaved();
    }

    private void assertRejected(Runnable call) {
        try {
            call.run();
            fail("expected InvalidTransactionException");
        } catch (InvalidTransactionException expected) {
            // expected
        }
    }

    private void assertBalancesUnchangedAndNothingSaved() {
        assertEquals(new BigDecimal("10.00"), primaryAccount.getAccountBalance());
        assertEquals(new BigDecimal("10.00"), savingsAccount.getAccountBalance());
        verify(primaryAccountDao, never()).save(any(PrimaryAccount.class));
        verify(savingsAccountDao, never()).save(any(SavingsAccount.class));
        verify(transactionService, never()).savePrimaryWithdrawTransaction(any());
        verify(transactionService, never()).saveSavingsWithdrawTransaction(any());
        verify(transactionService, never()).savePrimaryDepositTransaction(any());
        verify(transactionService, never()).saveSavingsDepositTransaction(any());
    }
}
