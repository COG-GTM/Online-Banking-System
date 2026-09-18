package com.userfront.service.UserServiceImpl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.userfront.dao.PrimaryAccountDao;
import com.userfront.dao.PrimaryTransactionDao;
import com.userfront.dao.RecipientDao;
import com.userfront.dao.SavingsAccountDao;
import com.userfront.dao.SavingsTransactionDao;
import com.userfront.domain.PrimaryAccount;
import com.userfront.domain.Recipient;
import com.userfront.domain.SavingsAccount;
import com.userfront.domain.User;
import com.userfront.exception.InsufficientFundsException;
import com.userfront.service.UserService;

@RunWith(MockitoJUnitRunner.Silent.class)
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

    @Before
    public void setUp() {
        primaryAccount = new PrimaryAccount();
        primaryAccount.setAccountBalance(new BigDecimal("100.00"));

        savingsAccount = new SavingsAccount();
        savingsAccount.setAccountBalance(new BigDecimal("0.00"));

        User user = new User();
        user.setPrimaryAccount(primaryAccount);
        user.setSavingsAccount(savingsAccount);

        when(userService.findByUsername("tester")).thenReturn(user);
    }

    @Test
    public void betweenAccountsTransferMovesTheAmount() throws Exception {
        transactionService.betweenAccountsTransfer("Primary", "Savings", new BigDecimal("10.55"), "tester");

        assertEquals(new BigDecimal("89.45"), primaryAccount.getAccountBalance());
        assertEquals(new BigDecimal("10.55"), savingsAccount.getAccountBalance());
    }

    @Test
    public void betweenAccountsTransferRejectsAmountAboveBalance() throws Exception {
        try {
            transactionService.betweenAccountsTransfer("Primary", "Savings", new BigDecimal("100.01"), "tester");
            fail("Expected InsufficientFundsException");
        } catch (InsufficientFundsException expected) {
            // expected
        }

        assertEquals(new BigDecimal("100.00"), primaryAccount.getAccountBalance());
        assertEquals(new BigDecimal("0.00"), savingsAccount.getAccountBalance());
        verify(primaryAccountDao, never()).save(primaryAccount);
        verify(savingsAccountDao, never()).save(savingsAccount);
    }

    @Test
    public void toSomeoneElseTransferRejectsAmountAboveBalance() {
        Recipient recipient = new Recipient();
        recipient.setName("Someone");

        try {
            transactionService.toSomeoneElseTransfer(recipient, "Primary", new BigDecimal("100.01"), "tester");
            fail("Expected InsufficientFundsException");
        } catch (InsufficientFundsException expected) {
            // expected
        }

        assertEquals(new BigDecimal("100.00"), primaryAccount.getAccountBalance());
        verify(primaryAccountDao, never()).save(primaryAccount);
    }
}
