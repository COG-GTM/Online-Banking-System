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
        when(primaryAccountDao.findByIdForUpdate(1L)).thenReturn(primaryAccount);
        when(savingsAccountDao.findByIdForUpdate(2L)).thenReturn(savingsAccount);
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
    public void negativeWithdrawDoesNotCreditAccount() {
        try {
            accountService.withdraw("Primary", new BigDecimal("-500000"), principal);
            fail("Expected rejection");
        } catch (IllegalArgumentException expected) {
            // expected
        }
        assertEquals(new BigDecimal("100.00"), primaryAccount.getAccountBalance());
        verify(primaryAccountDao, never()).save(any(PrimaryAccount.class));
        verify(transactionService, never()).savePrimaryWithdrawTransaction(any(PrimaryTransaction.class));
    }

    @Test
    public void negativeDepositIsRejected() {
        try {
            accountService.deposit("Savings", new BigDecimal("-10"), principal);
            fail("Expected rejection");
        } catch (IllegalArgumentException expected) {
            // expected
        }
        assertEquals(new BigDecimal("50.00"), savingsAccount.getAccountBalance());
        verify(transactionService, never()).saveSavingsDepositTransaction(any(SavingsTransaction.class));
    }

    @Test
    public void withdrawExceedingBalanceIsRejected() {
        try {
            accountService.withdraw("Savings", new BigDecimal("50.01"), principal);
            fail("Expected rejection");
        } catch (IllegalArgumentException expected) {
            // expected
        }
        assertEquals(new BigDecimal("50.00"), savingsAccount.getAccountBalance());
        verify(savingsAccountDao, never()).save(any(SavingsAccount.class));
    }

    @Test
    public void validWithdrawAndDepositUpdateBalance() {
        accountService.withdraw("Primary", new BigDecimal("100.00"), principal);
        assertEquals(new BigDecimal("0.00"), primaryAccount.getAccountBalance());

        accountService.deposit("Savings", new BigDecimal("25.5"), principal);
        assertEquals(new BigDecimal("75.50"), savingsAccount.getAccountBalance());

        verify(transactionService).savePrimaryWithdrawTransaction(any(PrimaryTransaction.class));
        verify(transactionService).saveSavingsDepositTransaction(any(SavingsTransaction.class));
    }

    @Test(expected = IllegalArgumentException.class)
    public void unknownAccountTypeIsRejected() {
        accountService.deposit("Checking", new BigDecimal("10"), principal);
    }
}
