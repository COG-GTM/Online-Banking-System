package com.userfront.service.UserServiceImpl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

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
import com.userfront.service.InvalidTransactionException;
import com.userfront.service.UserService;

@RunWith(MockitoJUnitRunner.class)
public class TransactionServiceImplTest {

    @Mock private UserService userService;
    @Mock private PrimaryTransactionDao primaryTransactionDao;
    @Mock private SavingsTransactionDao savingsTransactionDao;
    @Mock private PrimaryAccountDao primaryAccountDao;
    @Mock private SavingsAccountDao savingsAccountDao;
    @Mock private RecipientDao recipientDao;

    @InjectMocks private TransactionServiceImpl transactionService;

    private PrimaryAccount primary;
    private SavingsAccount savings;

    @Before
    public void setUp() {
        primary = new PrimaryAccount();
        primary.setAccountBalance(new BigDecimal("100.00"));
        savings = new SavingsAccount();
        savings.setAccountBalance(new BigDecimal("50.00"));
    }

    @Test
    public void betweenAccountsMovesExactAmount() {
        transactionService.betweenAccountsTransfer("Primary", "Savings", new BigDecimal("0.10"), primary, savings);

        assertEquals(new BigDecimal("99.90"), primary.getAccountBalance());
        assertEquals(new BigDecimal("50.10"), savings.getAccountBalance());
    }

    @Test
    public void betweenAccountsRejectsNegativeAmount() {
        expectRejected(() -> transactionService.betweenAccountsTransfer("Primary", "Savings", new BigDecimal("-500.00"), primary, savings));
    }

    @Test
    public void betweenAccountsRejectsOverdraft() {
        expectRejected(() -> transactionService.betweenAccountsTransfer("Savings", "Primary", new BigDecimal("50.01"), primary, savings));
    }

    @Test
    public void betweenAccountsRejectsSameAccount() {
        expectRejected(() -> transactionService.betweenAccountsTransfer("Primary", "Primary", new BigDecimal("1.00"), primary, savings));
    }

    @Test
    public void toSomeoneElseRejectsMissingRecipientBeforeDebiting() {
        expectRejected(() -> transactionService.toSomeoneElseTransfer(null, "Primary", new BigDecimal("10.00"), primary, savings));
    }

    @Test
    public void toSomeoneElseRejectsNegativeAmount() {
        expectRejected(() -> transactionService.toSomeoneElseTransfer(recipient(), "Primary", new BigDecimal("-10.00"), primary, savings));
    }

    @Test
    public void toSomeoneElseRejectsOverdraft() {
        expectRejected(() -> transactionService.toSomeoneElseTransfer(recipient(), "Savings", new BigDecimal("50.01"), primary, savings));
    }

    @Test
    public void toSomeoneElseRejectsUnknownAccountType() {
        expectRejected(() -> transactionService.toSomeoneElseTransfer(recipient(), "Checking", new BigDecimal("1.00"), primary, savings));
    }

    @Test
    public void toSomeoneElseDebitsSender() {
        transactionService.toSomeoneElseTransfer(recipient(), "Primary", new BigDecimal("25.25"), primary, savings);

        assertEquals(new BigDecimal("74.75"), primary.getAccountBalance());
        verify(primaryAccountDao).save(primary);
    }

    private Recipient recipient() {
        Recipient recipient = new Recipient();
        recipient.setName("Alice");
        return recipient;
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
