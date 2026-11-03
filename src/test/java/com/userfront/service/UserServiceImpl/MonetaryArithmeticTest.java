package com.userfront.service.UserServiceImpl;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.security.Principal;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import com.userfront.dao.PrimaryAccountDao;
import com.userfront.dao.PrimaryTransactionDao;
import com.userfront.dao.RecipientDao;
import com.userfront.dao.SavingsAccountDao;
import com.userfront.dao.SavingsTransactionDao;
import com.userfront.domain.Money;
import com.userfront.domain.PrimaryAccount;
import com.userfront.domain.PrimaryTransaction;
import com.userfront.domain.Recipient;
import com.userfront.domain.SavingsAccount;
import com.userfront.domain.SavingsTransaction;
import com.userfront.domain.User;
import com.userfront.service.TransactionService;
import com.userfront.service.UserService;

public class MonetaryArithmeticTest {

    private AccountServiceImpl accountService;
    private TransactionServiceImpl transactionService;
    private TransactionService transactionServiceMock;
    private PrimaryTransactionDao primaryTransactionDao;
    private SavingsTransactionDao savingsTransactionDao;
    private PrimaryAccount primaryAccount;
    private SavingsAccount savingsAccount;
    private Principal principal;

    @Before
    public void setUp() {
        primaryAccount = new PrimaryAccount();
        primaryAccount.setAccountBalance(Money.ZERO);
        savingsAccount = new SavingsAccount();
        savingsAccount.setAccountBalance(Money.ZERO);

        User user = new User();
        user.setUsername("alice");
        user.setPrimaryAccount(primaryAccount);
        user.setSavingsAccount(savingsAccount);

        UserService userService = mock(UserService.class);
        when(userService.findByUsername("alice")).thenReturn(user);
        principal = () -> "alice";

        transactionServiceMock = mock(TransactionService.class);
        accountService = new AccountServiceImpl();
        ReflectionTestUtils.setField(accountService, "userService", userService);
        ReflectionTestUtils.setField(accountService, "transactionService", transactionServiceMock);
        ReflectionTestUtils.setField(accountService, "primaryAccountDao", mock(PrimaryAccountDao.class));
        ReflectionTestUtils.setField(accountService, "savingsAccountDao", mock(SavingsAccountDao.class));

        primaryTransactionDao = mock(PrimaryTransactionDao.class);
        savingsTransactionDao = mock(SavingsTransactionDao.class);
        transactionService = new TransactionServiceImpl();
        ReflectionTestUtils.setField(transactionService, "primaryTransactionDao", primaryTransactionDao);
        ReflectionTestUtils.setField(transactionService, "savingsTransactionDao", savingsTransactionDao);
        ReflectionTestUtils.setField(transactionService, "primaryAccountDao", mock(PrimaryAccountDao.class));
        ReflectionTestUtils.setField(transactionService, "savingsAccountDao", mock(SavingsAccountDao.class));
        ReflectionTestUtils.setField(transactionService, "recipientDao", mock(RecipientDao.class));
    }

    @Test
    public void depositsOfOneTenthAccumulateExactly() {
        for (int i = 0; i < 10; i++) {
            accountService.deposit("Primary", Money.parse("0.1"), principal);
        }

        assertEquals("1.00", primaryAccount.getAccountBalance().toPlainString());
    }

    @Test
    public void depositRecordsExactAmountAndBalanceOnTheLedger() {
        accountService.deposit("Savings", Money.parse("12345678901234567.89"), principal);

        ArgumentCaptor<SavingsTransaction> captor = ArgumentCaptor.forClass(SavingsTransaction.class);
        verify(transactionServiceMock).saveSavingsDepositTransaction(captor.capture());
        assertEquals("12345678901234567.89", captor.getValue().getAmount().toPlainString());
        assertEquals("12345678901234567.89", captor.getValue().getAvailableBalance().toPlainString());
        assertEquals("12345678901234567.89", savingsAccount.getAccountBalance().toPlainString());
    }

    @Test
    public void withdrawKeepsFixedScale() {
        primaryAccount.setAccountBalance(Money.parse("100"));

        accountService.withdraw("Primary", new BigDecimal("33.333"), principal);

        assertEquals("66.67", primaryAccount.getAccountBalance().toPlainString());
        ArgumentCaptor<PrimaryTransaction> captor = ArgumentCaptor.forClass(PrimaryTransaction.class);
        verify(transactionServiceMock).savePrimaryWithdrawTransaction(captor.capture());
        assertEquals("33.33", captor.getValue().getAmount().toPlainString());
    }

    @Test
    public void betweenAccountsTransferUsesExactDecimalArithmetic() throws Exception {
        primaryAccount.setAccountBalance(Money.parse("0.30"));

        transactionService.betweenAccountsTransfer("Primary", "Savings", "0.1", primaryAccount, savingsAccount);
        transactionService.betweenAccountsTransfer("Primary", "Savings", "0.2", primaryAccount, savingsAccount);

        assertEquals("0.00", primaryAccount.getAccountBalance().toPlainString());
        assertEquals("0.30", savingsAccount.getAccountBalance().toPlainString());
        ArgumentCaptor<PrimaryTransaction> captor = ArgumentCaptor.forClass(PrimaryTransaction.class);
        verify(primaryTransactionDao, org.mockito.Mockito.times(2)).save(captor.capture());
        assertEquals("0.20", captor.getAllValues().get(1).getAmount().toPlainString());
        assertEquals("0.00", captor.getAllValues().get(1).getAvailableBalance().toPlainString());
    }

    @Test
    public void toSomeoneElseTransferRecordsExactAmount() {
        savingsAccount.setAccountBalance(Money.parse("10000000000000000.05"));
        Recipient recipient = new Recipient();
        recipient.setName("bob");

        transactionService.toSomeoneElseTransfer(recipient, "Savings", "0.03", primaryAccount, savingsAccount);

        assertEquals("10000000000000000.02", savingsAccount.getAccountBalance().toPlainString());
        ArgumentCaptor<SavingsTransaction> captor = ArgumentCaptor.forClass(SavingsTransaction.class);
        verify(savingsTransactionDao).save(captor.capture());
        assertEquals("0.03", captor.getValue().getAmount().toPlainString());
    }

    @Test(expected = IllegalArgumentException.class)
    public void transferRejectsMalformedAmountBeforeTouchingBalances() throws Exception {
        try {
            transactionService.betweenAccountsTransfer("Primary", "Savings", "abc", primaryAccount, savingsAccount);
        } finally {
            assertEquals("0.00", primaryAccount.getAccountBalance().toPlainString());
        }
    }
}
