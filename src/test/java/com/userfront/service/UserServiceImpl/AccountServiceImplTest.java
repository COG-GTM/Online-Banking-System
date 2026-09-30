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
import com.userfront.service.TransactionService;
import com.userfront.service.UserService;
import com.userfront.validation.InvalidAmountException;

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

    @Before
    public void setUp() {
        primaryAccount = new PrimaryAccount();
        primaryAccount.setAccountBalance(new BigDecimal("100.00"));
        SavingsAccount savingsAccount = new SavingsAccount();
        savingsAccount.setAccountBalance(new BigDecimal("100.00"));
        User user = new User();
        user.setPrimaryAccount(primaryAccount);
        user.setSavingsAccount(savingsAccount);
        when(userService.findByUsername("alice")).thenReturn(user);
    }

    @Test
    public void depositAddsPositiveAmount() {
        accountService.deposit("Primary", new BigDecimal("0.10"), principal);

        assertEquals(new BigDecimal("100.10"), primaryAccount.getAccountBalance());
    }

    @Test
    public void depositAndWithdrawRejectNegativeAmounts() {
        try {
            accountService.deposit("Primary", new BigDecimal("-500"), principal);
            fail("Expected InvalidAmountException");
        } catch (InvalidAmountException expected) {
            // expected
        }
        try {
            accountService.withdraw("Primary", new BigDecimal("-500"), principal);
            fail("Expected InvalidAmountException");
        } catch (InvalidAmountException expected) {
            // expected
        }

        assertEquals(new BigDecimal("100.00"), primaryAccount.getAccountBalance());
        verify(primaryAccountDao, never()).save(any(PrimaryAccount.class));
    }
}
