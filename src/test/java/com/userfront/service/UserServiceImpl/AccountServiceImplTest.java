package com.userfront.service.UserServiceImpl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.security.Principal;

import javax.persistence.EntityManager;
import javax.persistence.LockModeType;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
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

    private final Principal principal = () -> "alice";
    private PrimaryAccount primary;
    private SavingsAccount savings;

    @Before
    public void setUp() {
        primary = new PrimaryAccount();
        primary.setId(1L);
        primary.setAccountBalance(new BigDecimal("100.00"));
        savings = new SavingsAccount();
        savings.setId(2L);
        savings.setAccountBalance(new BigDecimal("50.00"));

        User user = new User();
        user.setPrimaryAccount(primary);
        user.setSavingsAccount(savings);
        when(userService.findByUsername("alice")).thenReturn(user);
        when(entityManager.find(PrimaryAccount.class, 1L)).thenReturn(primary);
        when(entityManager.find(SavingsAccount.class, 2L)).thenReturn(savings);
    }

    @Test
    public void depositAddsAmountUnderRowLock() {
        accountService.deposit("Primary", new BigDecimal("25.5"), principal);

        assertEquals(new BigDecimal("125.50"), primary.getAccountBalance());
        verify(entityManager).refresh(primary, LockModeType.PESSIMISTIC_WRITE);
        ArgumentCaptor<PrimaryTransaction> tx = ArgumentCaptor.forClass(PrimaryTransaction.class);
        verify(transactionService).savePrimaryDepositTransaction(tx.capture());
        assertEquals(25.5, tx.getValue().getAmount(), 0.0);
        assertEquals(new BigDecimal("125.50"), tx.getValue().getAvailableBalance());
    }

    @Test
    public void withdrawWithinBalanceSubtracts() {
        accountService.withdraw("Savings", new BigDecimal("50.00"), principal);

        assertEquals(new BigDecimal("0.00"), savings.getAccountBalance());
        verify(entityManager).refresh(savings, LockModeType.PESSIMISTIC_WRITE);
        verify(transactionService).saveSavingsWithdrawTransaction(any(SavingsTransaction.class));
    }

    @Test
    public void negativeWithdrawalIsRejectedAndDoesNotCredit() {
        assertRejected(() -> accountService.withdraw("Primary", new BigDecimal("-500000"), principal),
                InvalidTransactionException.class);
        assertUnchanged();
    }

    @Test
    public void negativeAndZeroDepositsAreRejected() {
        assertRejected(() -> accountService.deposit("Primary", new BigDecimal("-10"), principal),
                InvalidTransactionException.class);
        assertRejected(() -> accountService.deposit("Savings", BigDecimal.ZERO, principal),
                InvalidTransactionException.class);
        assertRejected(() -> accountService.deposit("Savings", null, principal),
                InvalidTransactionException.class);
        assertUnchanged();
    }

    @Test
    public void overdraftIsRejected() {
        assertRejected(() -> accountService.withdraw("Primary", new BigDecimal("100.01"), principal),
                InsufficientFundsException.class);
        assertRejected(() -> accountService.withdraw("Savings", new BigDecimal("50.01"), principal),
                InsufficientFundsException.class);
        assertEquals(new BigDecimal("100.00"), primary.getAccountBalance());
        assertEquals(new BigDecimal("50.00"), savings.getAccountBalance());
        verify(primaryAccountDao, never()).save(any(PrimaryAccount.class));
        verify(savingsAccountDao, never()).save(any(SavingsAccount.class));
    }

    @Test
    public void ledgerRowIsSavedBeforeTheRowLockIsTaken() {
        accountService.withdraw("Primary", new BigDecimal("10"), principal);

        InOrder order = inOrder(transactionService, entityManager);
        ArgumentCaptor<PrimaryTransaction> tx = ArgumentCaptor.forClass(PrimaryTransaction.class);
        order.verify(transactionService).savePrimaryWithdrawTransaction(tx.capture());
        order.verify(entityManager).refresh(primary, LockModeType.PESSIMISTIC_WRITE);
        assertEquals(new BigDecimal("90.00"), tx.getValue().getAvailableBalance());
    }

    @Test
    public void unboundedAmountIsRejected() {
        assertRejected(() -> accountService.deposit("Primary", new BigDecimal("1000000.01"), principal),
                InvalidTransactionException.class);
        assertUnchanged();
    }

    @Test
    public void unknownAccountTypeIsRejected() {
        assertRejected(() -> accountService.deposit("Checking", new BigDecimal("5"), principal),
                InvalidTransactionException.class);
        assertUnchanged();
    }

    private void assertUnchanged() {
        assertEquals(new BigDecimal("100.00"), primary.getAccountBalance());
        assertEquals(new BigDecimal("50.00"), savings.getAccountBalance());
        verify(primaryAccountDao, never()).save(any(PrimaryAccount.class));
        verify(savingsAccountDao, never()).save(any(SavingsAccount.class));
        verify(transactionService, never()).savePrimaryDepositTransaction(any());
        verify(transactionService, never()).savePrimaryWithdrawTransaction(any());
        verify(transactionService, never()).saveSavingsDepositTransaction(any());
        verify(transactionService, never()).saveSavingsWithdrawTransaction(any());
    }

    private static void assertRejected(Runnable call, Class<? extends InvalidTransactionException> type) {
        try {
            call.run();
            fail("expected " + type.getSimpleName());
        } catch (InvalidTransactionException e) {
            assertEquals(type, e.getClass());
        }
    }
}
