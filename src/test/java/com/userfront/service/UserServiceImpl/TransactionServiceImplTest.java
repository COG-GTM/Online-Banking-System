package com.userfront.service.UserServiceImpl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
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
import com.userfront.service.TransactionRejectedException;
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
    @Mock
    private AccountLocker accountLocker;

    @InjectMocks
    private TransactionServiceImpl transactionService;

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
        when(accountLocker.lock(any(PrimaryAccount.class))).thenReturn(primaryAccount);
        when(accountLocker.lock(any(SavingsAccount.class))).thenReturn(savingsAccount);
    }

    @Test
    public void betweenAccountsTransferMovesExactAmount() {
        transactionService.betweenAccountsTransfer("Primary", "Savings", new BigDecimal("40.10"), primaryAccount, savingsAccount);

        assertEquals(new BigDecimal("59.90"), primaryAccount.getAccountBalance());
        assertEquals(new BigDecimal("90.10"), savingsAccount.getAccountBalance());
    }

    @Test
    public void betweenAccountsTransferRejectsNegativeAmount() {
        try {
            transactionService.betweenAccountsTransfer("Primary", "Savings", new BigDecimal("-500"), primaryAccount, savingsAccount);
            fail("expected rejection");
        } catch (TransactionRejectedException expected) {
        }
        assertUnchanged();
    }

    @Test
    public void betweenAccountsTransferRejectsOverdraft() {
        try {
            transactionService.betweenAccountsTransfer("Savings", "Primary", new BigDecimal("50.01"), primaryAccount, savingsAccount);
            fail("expected rejection");
        } catch (TransactionRejectedException expected) {
        }
        assertUnchanged();
    }

    @Test
    public void toSomeoneElseTransferRejectsOverdraft() {
        Recipient recipient = new Recipient();
        recipient.setName("payee");
        try {
            transactionService.toSomeoneElseTransfer(recipient, "Primary", new BigDecimal("100.01"), primaryAccount, savingsAccount);
            fail("expected rejection");
        } catch (TransactionRejectedException expected) {
        }
        assertUnchanged();
    }

    private void assertUnchanged() {
        assertEquals(new BigDecimal("100.00"), primaryAccount.getAccountBalance());
        assertEquals(new BigDecimal("50.00"), savingsAccount.getAccountBalance());
        verify(primaryAccountDao, never()).save(any(PrimaryAccount.class));
        verify(savingsAccountDao, never()).save(any(SavingsAccount.class));
    }
}
