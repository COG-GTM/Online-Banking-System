package com.userfront.service.UserServiceImpl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
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
import com.userfront.domain.SavingsAccount;
import com.userfront.domain.User;
import com.userfront.exception.InsufficientFundsException;
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
    private Principal principal;

    @InjectMocks
    private AccountServiceImpl accountService;

    private PrimaryAccount primaryAccount;

    @Before
    public void setUp() {
        primaryAccount = new PrimaryAccount();
        primaryAccount.setAccountBalance(new BigDecimal("100.00"));

        SavingsAccount savingsAccount = new SavingsAccount();
        savingsAccount.setAccountBalance(new BigDecimal("0.00"));

        User user = new User();
        user.setPrimaryAccount(primaryAccount);
        user.setSavingsAccount(savingsAccount);

        when(principal.getName()).thenReturn("tester");
        when(userService.findByUsername("tester")).thenReturn(user);
    }

    @Test
    public void withdrawDebitsTheAccount() throws InsufficientFundsException {
        accountService.withdraw("Primary", new BigDecimal("10.55"), principal);

        assertEquals(new BigDecimal("89.45"), primaryAccount.getAccountBalance());
        verify(primaryAccountDao).save(primaryAccount);
    }

    @Test
    public void withdrawRejectsNegativeAmountInsteadOfCreditingTheAccount() throws InsufficientFundsException {
        try {
            accountService.withdraw("Primary", new BigDecimal("-10.00"), principal);
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
            // expected
        }

        assertEquals(new BigDecimal("100.00"), primaryAccount.getAccountBalance());
        verify(primaryAccountDao, never()).save(primaryAccount);
    }

    @Test
    public void withdrawRejectsAmountAboveBalanceWithoutTouchingTheBalance() {
        try {
            accountService.withdraw("Primary", new BigDecimal("100.01"), principal);
            fail("Expected InsufficientFundsException");
        } catch (InsufficientFundsException expected) {
            // expected
        }

        assertEquals(new BigDecimal("100.00"), primaryAccount.getAccountBalance());
        verify(primaryAccountDao, never()).save(primaryAccount);
        verifyNoMoreInteractions(transactionService);
    }
}
