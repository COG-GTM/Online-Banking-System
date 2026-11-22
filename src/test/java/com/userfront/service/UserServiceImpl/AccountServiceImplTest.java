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
import com.userfront.domain.SavingsAccount;
import com.userfront.domain.User;
import com.userfront.service.InvalidTransactionException;
import com.userfront.service.TransactionService;
import com.userfront.service.UserService;

@RunWith(MockitoJUnitRunner.Silent.class)
public class AccountServiceImplTest {

    @Mock private PrimaryAccountDao primaryAccountDao;
    @Mock private SavingsAccountDao savingsAccountDao;
    @Mock private UserService userService;
    @Mock private TransactionService transactionService;

    @InjectMocks private AccountServiceImpl accountService;

    private final Principal principal = () -> "alice";
    private PrimaryAccount primary;
    private SavingsAccount savings;

    @Before
    public void setUp() {
        primary = new PrimaryAccount();
        primary.setAccountBalance(new BigDecimal("100.00"));
        savings = new SavingsAccount();
        savings.setAccountBalance(new BigDecimal("50.00"));
        User user = new User();
        user.setPrimaryAccount(primary);
        user.setSavingsAccount(savings);
        when(userService.findByUsername("alice")).thenReturn(user);
    }

    @Test
    public void depositAddsExactAmount() {
        accountService.deposit("Primary", new BigDecimal("0.10"), principal);

        assertEquals(new BigDecimal("100.10"), primary.getAccountBalance());
    }

    @Test
    public void depositRejectsNegativeAmount() {
        expectRejected(() -> accountService.deposit("Primary", new BigDecimal("-500.00"), principal));
    }

    @Test
    public void withdrawRejectsNegativeAmount() {
        expectRejected(() -> accountService.withdraw("Primary", new BigDecimal("-500.00"), principal));
    }

    @Test
    public void withdrawRejectsOverdraft() {
        expectRejected(() -> accountService.withdraw("Savings", new BigDecimal("50.01"), principal));
    }

    @Test
    public void withdrawRejectsUnknownAccountType() {
        expectRejected(() -> accountService.withdraw("Checking", new BigDecimal("1.00"), principal));
    }

    @Test
    public void withdrawAllowsFullBalance() {
        accountService.withdraw("Savings", new BigDecimal("50.00"), principal);

        assertEquals(new BigDecimal("0.00"), savings.getAccountBalance());
    }

    private void expectRejected(Runnable action) {
        try {
            action.run();
            fail("Expected InvalidTransactionException");
        } catch (InvalidTransactionException expected) {
            assertEquals(new BigDecimal("100.00"), primary.getAccountBalance());
            assertEquals(new BigDecimal("50.00"), savings.getAccountBalance());
            verify(primaryAccountDao, never()).save(any());
            verify(savingsAccountDao, never()).save(any());
        }
    }
}
