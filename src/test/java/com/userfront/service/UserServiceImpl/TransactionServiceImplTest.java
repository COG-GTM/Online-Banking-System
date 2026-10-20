package com.userfront.service.UserServiceImpl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;

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
import com.userfront.domain.PrimaryTransaction;
import com.userfront.domain.Recipient;
import com.userfront.domain.SavingsAccount;
import com.userfront.domain.SavingsTransaction;
import com.userfront.domain.User;
import com.userfront.service.InvalidTransferException;
import com.userfront.service.UserService;

@RunWith(MockitoJUnitRunner.class)
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
    private Recipient ownRecipient;

    @Before
    public void setUp() {
        primaryAccount = new PrimaryAccount();
        primaryAccount.setId(1L);
        primaryAccount.setAccountBalance(new BigDecimal("100.00"));

        savingsAccount = new SavingsAccount();
        savingsAccount.setId(2L);
        savingsAccount.setAccountBalance(new BigDecimal("50.00"));

        User owner = new User();
        owner.setPrimaryAccount(primaryAccount);
        owner.setSavingsAccount(savingsAccount);

        ownRecipient = new Recipient();
        ownRecipient.setName("Alice");
        ownRecipient.setUser(owner);
    }

    @Test
    public void toSomeoneElseRejectsNegativeAmount() {
        expectRejected(() -> transactionService.toSomeoneElseTransfer(ownRecipient, "Primary", "-1000000", primaryAccount, savingsAccount));
        expectRejected(() -> transactionService.toSomeoneElseTransfer(ownRecipient, "Savings", "-1000000", primaryAccount, savingsAccount));
        assertBalancesUnchanged();
    }

    @Test
    public void toSomeoneElseRejectsZeroAndMalformedAmounts() {
        for (String amount : new String[] {"0", "0.00", "", "  ", null, "abc", "NaN", "1e3x", "10.001"}) {
            expectRejected(() -> transactionService.toSomeoneElseTransfer(ownRecipient, "Primary", amount, primaryAccount, savingsAccount));
        }
        assertBalancesUnchanged();
    }

    @Test
    public void toSomeoneElseRejectsOverdraft() {
        expectRejected(() -> transactionService.toSomeoneElseTransfer(ownRecipient, "Primary", "100.01", primaryAccount, savingsAccount));
        expectRejected(() -> transactionService.toSomeoneElseTransfer(ownRecipient, "Savings", "50.01", primaryAccount, savingsAccount));
        assertBalancesUnchanged();
    }

    @Test
    public void toSomeoneElseRejectsMissingRecipientBeforeMutatingBalance() {
        expectRejected(() -> transactionService.toSomeoneElseTransfer(null, "Primary", "10", primaryAccount, savingsAccount));
        assertBalancesUnchanged();
    }

    @Test
    public void toSomeoneElseRejectsRecipientOwnedByAnotherUser() {
        PrimaryAccount otherAccount = new PrimaryAccount();
        otherAccount.setId(99L);
        User otherUser = new User();
        otherUser.setPrimaryAccount(otherAccount);
        Recipient foreignRecipient = new Recipient();
        foreignRecipient.setName("Mallory");
        foreignRecipient.setUser(otherUser);

        expectRejected(() -> transactionService.toSomeoneElseTransfer(foreignRecipient, "Primary", "10", primaryAccount, savingsAccount));
        assertBalancesUnchanged();
    }

    @Test
    public void toSomeoneElseRejectsUnknownAccountType() {
        expectRejected(() -> transactionService.toSomeoneElseTransfer(ownRecipient, "Checking", "10", primaryAccount, savingsAccount));
        assertBalancesUnchanged();
    }

    @Test
    public void toSomeoneElseDebitsValidAmount() {
        transactionService.toSomeoneElseTransfer(ownRecipient, "Primary", "40.50", primaryAccount, savingsAccount);

        assertEquals(0, new BigDecimal("59.50").compareTo(primaryAccount.getAccountBalance()));
        verify(primaryAccountDao).save(primaryAccount);
        verify(primaryTransactionDao).save(any(PrimaryTransaction.class));
    }

    @Test
    public void betweenAccountsRejectsNegativeAmount() {
        expectRejected(() -> transactionService.betweenAccountsTransfer("Primary", "Savings", "-500", primaryAccount, savingsAccount));
        expectRejected(() -> transactionService.betweenAccountsTransfer("Savings", "Primary", "-500", primaryAccount, savingsAccount));
        assertBalancesUnchanged();
    }

    @Test
    public void betweenAccountsRejectsOverdraft() {
        expectRejected(() -> transactionService.betweenAccountsTransfer("Primary", "Savings", "100.01", primaryAccount, savingsAccount));
        expectRejected(() -> transactionService.betweenAccountsTransfer("Savings", "Primary", "50.01", primaryAccount, savingsAccount));
        assertBalancesUnchanged();
    }

    @Test
    public void betweenAccountsMovesValidAmount() throws Exception {
        transactionService.betweenAccountsTransfer("Savings", "Primary", "50", primaryAccount, savingsAccount);

        assertEquals(0, new BigDecimal("150.00").compareTo(primaryAccount.getAccountBalance()));
        assertEquals(0, BigDecimal.ZERO.compareTo(savingsAccount.getAccountBalance()));
        verify(savingsTransactionDao, times(1)).save(any(SavingsTransaction.class));
    }

    private interface Transfer {
        void run() throws Exception;
    }

    private void expectRejected(Transfer transfer) {
        try {
            transfer.run();
            fail("Expected InvalidTransferException");
        } catch (InvalidTransferException expected) {
            // expected
        } catch (Exception e) {
            fail("Expected InvalidTransferException but got " + e);
        }
    }

    private void assertBalancesUnchanged() {
        assertEquals(0, new BigDecimal("100.00").compareTo(primaryAccount.getAccountBalance()));
        assertEquals(0, new BigDecimal("50.00").compareTo(savingsAccount.getAccountBalance()));
        verifyZeroInteractions(primaryAccountDao, savingsAccountDao, primaryTransactionDao, savingsTransactionDao);
    }
}
