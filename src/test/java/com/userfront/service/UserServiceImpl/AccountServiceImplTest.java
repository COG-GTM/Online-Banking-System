package com.userfront.service.UserServiceImpl;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.security.Principal;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

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

public class AccountServiceImplTest {

    private final Principal principal = () -> "alice";

    private AccountServiceImpl service;
    private PrimaryAccountDao primaryAccountDao;
    private SavingsAccountDao savingsAccountDao;
    private TransactionService transactionService;
    private PrimaryAccount primaryAccount;
    private SavingsAccount savingsAccount;

    @Before
    public void setUp() {
        primaryAccountDao = mock(PrimaryAccountDao.class);
        savingsAccountDao = mock(SavingsAccountDao.class);
        transactionService = mock(TransactionService.class);
        UserService userService = mock(UserService.class);

        primaryAccount = new PrimaryAccount();
        primaryAccount.setId(1L);
        primaryAccount.setAccountBalance(new BigDecimal("100.00"));
        savingsAccount = new SavingsAccount();
        savingsAccount.setId(2L);
        savingsAccount.setAccountBalance(new BigDecimal("50.00"));
        User user = new User();
        user.setPrimaryAccount(primaryAccount);
        user.setSavingsAccount(savingsAccount);
        when(userService.findByUsername("alice")).thenReturn(user);

        service = new AccountServiceImpl();
        ReflectionTestUtils.setField(service, "primaryAccountDao", primaryAccountDao);
        ReflectionTestUtils.setField(service, "savingsAccountDao", savingsAccountDao);
        ReflectionTestUtils.setField(service, "userService", userService);
        ReflectionTestUtils.setField(service, "transactionService", transactionService);
    }

    @Test
    public void negativeWithdrawIsRejectedBeforeTouchingTheBalance() {
        try {
            service.withdraw("Primary", new BigDecimal("-1000000"), principal);
        } catch (InvalidTransactionException expected) {
            verifyNoMoreInteractions(primaryAccountDao, savingsAccountDao, transactionService);
            return;
        }
        throw new AssertionError("negative withdraw accepted");
    }

    @Test
    public void negativeDepositIsRejectedBeforeTouchingTheBalance() {
        try {
            service.deposit("Savings", new BigDecimal("-5"), principal);
        } catch (InvalidTransactionException expected) {
            verifyNoMoreInteractions(primaryAccountDao, savingsAccountDao, transactionService);
            return;
        }
        throw new AssertionError("negative deposit accepted");
    }

    @Test
    public void overdraftWithdrawThrowsAndRecordsNoTransaction() {
        when(primaryAccountDao.debitIfCovered(1L, new BigDecimal("100.01"))).thenReturn(0);

        try {
            service.withdraw("Primary", new BigDecimal("100.01"), principal);
            throw new AssertionError("overdraft accepted");
        } catch (InsufficientFundsException expected) {
        }

        verify(transactionService, never()).savePrimaryWithdrawTransaction(any(PrimaryTransaction.class));
        verify(primaryAccountDao, never()).save(any(PrimaryAccount.class));
    }

    @Test
    public void coveredWithdrawDebitsAtomicallyAndRecordsNewBalance() {
        when(savingsAccountDao.debitIfCovered(2L, new BigDecimal("20.00"))).thenReturn(1);
        when(savingsAccountDao.findBalanceById(2L)).thenReturn(new BigDecimal("30.00"));

        service.withdraw("Savings", new BigDecimal("20"), principal);

        ArgumentCaptor<SavingsTransaction> tx = ArgumentCaptor.forClass(SavingsTransaction.class);
        verify(transactionService).saveSavingsWithdrawTransaction(tx.capture());
        assertEquals(20.0, tx.getValue().getAmount(), 0.0);
        assertEquals(new BigDecimal("30.00"), tx.getValue().getAvailableBalance());
        verify(savingsAccountDao, never()).save(any(SavingsAccount.class));
    }

    @Test
    public void depositCreditsAndRecordsNewBalance() {
        when(primaryAccountDao.credit(1L, new BigDecimal("25.50"))).thenReturn(1);
        when(primaryAccountDao.findBalanceById(1L)).thenReturn(new BigDecimal("125.50"));

        service.deposit("primary", new BigDecimal("25.5"), principal);

        ArgumentCaptor<PrimaryTransaction> tx = ArgumentCaptor.forClass(PrimaryTransaction.class);
        verify(transactionService).savePrimaryDepositTransaction(tx.capture());
        assertEquals(new BigDecimal("125.50"), tx.getValue().getAvailableBalance());
    }

    @Test(expected = InvalidTransactionException.class)
    public void unknownAccountTypeIsRejected() {
        try {
            service.withdraw("Checking", new BigDecimal("1"), principal);
        } finally {
            verify(primaryAccountDao, never()).debitIfCovered(anyLong(), any(BigDecimal.class));
            verify(savingsAccountDao, never()).debitIfCovered(anyLong(), any(BigDecimal.class));
        }
    }
}
