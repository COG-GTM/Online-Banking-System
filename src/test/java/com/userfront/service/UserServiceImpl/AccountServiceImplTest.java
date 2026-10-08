package com.userfront.service.UserServiceImpl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
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

    @InjectMocks
    private AccountServiceImpl accountService;

    private final Principal principal = () -> "alice";
    private PrimaryAccount primaryAccount;
    private SavingsAccount savingsAccount;

    @Before
    public void setUp() {
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
        when(primaryAccountDao.findBalanceById(1L)).thenReturn(new BigDecimal("0.00"));
        when(savingsAccountDao.findBalanceById(2L)).thenReturn(new BigDecimal("75.50"));
    }

    @Test
    public void parseAmountRejectsNonFiniteAndMalformedInput() {
        for (String input : new String[] {null, "", "  ", "NaN", "Infinity", "-Infinity", "abc", "1e400", "0", "-500000", "0.001", "1000000.01"}) {
            try {
                AccountServiceImpl.parseAmount(input);
                fail("Expected rejection for " + input);
            } catch (IllegalArgumentException expected) {
                // expected
            }
        }
    }

    @Test
    public void parseAmountNormalizesToTwoDecimals() {
        assertEquals(new BigDecimal("12.50"), AccountServiceImpl.parseAmount(" 12.5 "));
    }

    @Test
    public void negativeWithdrawNeverReachesTheBalanceUpdate() {
        try {
            accountService.withdraw("Primary", new BigDecimal("-500000"), principal);
            fail("Expected rejection");
        } catch (IllegalArgumentException expected) {
            // expected
        }
        verifyZeroInteractions(primaryAccountDao, savingsAccountDao, transactionService);
    }

    @Test
    public void negativeDepositNeverReachesTheBalanceUpdate() {
        try {
            accountService.deposit("Savings", new BigDecimal("-10"), principal);
            fail("Expected rejection");
        } catch (IllegalArgumentException expected) {
            // expected
        }
        verifyZeroInteractions(primaryAccountDao, savingsAccountDao, transactionService);
    }

    @Test
    public void withdrawRejectedWhenConditionalDebitMatchesNoRow() {
        when(savingsAccountDao.debitBalanceIfSufficient(2L, new BigDecimal("50.01"))).thenReturn(0);
        try {
            accountService.withdraw("Savings", new BigDecimal("50.01"), principal);
            fail("Expected rejection");
        } catch (IllegalArgumentException expected) {
            assertEquals("Insufficient funds.", expected.getMessage());
        }
        verify(savingsAccountDao).debitBalanceIfSufficient(2L, new BigDecimal("50.01"));
        verify(savingsAccountDao, never()).findBalanceById(2L);
    }

    @Test
    public void validWithdrawAndDepositUseAtomicBalanceUpdates() {
        when(primaryAccountDao.debitBalanceIfSufficient(1L, new BigDecimal("100.00"))).thenReturn(1);
        when(savingsAccountDao.creditBalance(2L, new BigDecimal("25.50"))).thenReturn(1);

        accountService.withdraw("Primary", new BigDecimal("100"), principal);
        accountService.deposit("Savings", new BigDecimal("25.5"), principal);

        verify(primaryAccountDao).debitBalanceIfSufficient(1L, new BigDecimal("100.00"));
        verify(savingsAccountDao).creditBalance(2L, new BigDecimal("25.50"));
        ArgumentCaptor<PrimaryTransaction> withdrawal = ArgumentCaptor.forClass(PrimaryTransaction.class);
        ArgumentCaptor<SavingsTransaction> deposit = ArgumentCaptor.forClass(SavingsTransaction.class);
        verify(transactionService).savePrimaryWithdrawTransaction(withdrawal.capture());
        verify(transactionService).saveSavingsDepositTransaction(deposit.capture());
        assertEquals(new BigDecimal("0.00"), withdrawal.getValue().getAvailableBalance());
        assertEquals(new BigDecimal("75.50"), deposit.getValue().getAvailableBalance());
    }

    @Test(expected = IllegalArgumentException.class)
    public void unknownAccountTypeIsRejected() {
        accountService.deposit("Checking", new BigDecimal("10"), principal);
    }
}
