package com.userfront.service.UserServiceImpl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.math.BigDecimal;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
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
import com.userfront.domain.User;
import com.userfront.service.UserService;
import com.userfront.service.transfer.InsufficientFundsException;
import com.userfront.service.transfer.InvalidTransferException;

@RunWith(MockitoJUnitRunner.class)
public class TransactionServiceImplTransferTest {

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
    private TransactionServiceImpl service;

    private PrimaryAccount primary;
    private SavingsAccount savings;
    private Recipient ownRecipient;

    @Before
    public void setUp() {
        primary = new PrimaryAccount();
        primary.setId(1L);
        primary.setAccountBalance(new BigDecimal("100.00"));
        savings = new SavingsAccount();
        savings.setId(2L);
        savings.setAccountBalance(new BigDecimal("50.00"));

        User owner = new User();
        owner.setPrimaryAccount(primary);
        owner.setSavingsAccount(savings);
        ownRecipient = new Recipient();
        ownRecipient.setName("Bob");
        ownRecipient.setUser(owner);
    }

    @Test
    public void toSomeoneElseRejectsNegativeAmountWithoutTouchingBalance() {
        expectInvalid(() -> service.toSomeoneElseTransfer(ownRecipient, "Primary", "-1000000", primary, savings));

        assertEquals(new BigDecimal("100.00"), primary.getAccountBalance());
        verifyNoInteractions(primaryAccountDao, primaryTransactionDao, savingsAccountDao, savingsTransactionDao);
    }

    @Test
    public void toSomeoneElseRejectsOverdraft() {
        try {
            service.toSomeoneElseTransfer(ownRecipient, "Savings", "50.01", primary, savings);
            fail("expected InsufficientFundsException");
        } catch (InsufficientFundsException expected) {
        }

        assertEquals(new BigDecimal("50.00"), savings.getAccountBalance());
        verifyNoInteractions(savingsAccountDao, savingsTransactionDao);
    }

    @Test
    public void toSomeoneElseRejectsMissingRecipientBeforeMutating() {
        expectInvalid(() -> service.toSomeoneElseTransfer(null, "Primary", "10", primary, savings));

        assertEquals(new BigDecimal("100.00"), primary.getAccountBalance());
        verifyNoInteractions(primaryAccountDao, primaryTransactionDao);
    }

    @Test
    public void toSomeoneElseRejectsAnotherUsersRecipient() {
        PrimaryAccount otherPrimary = new PrimaryAccount();
        otherPrimary.setId(99L);
        User other = new User();
        other.setPrimaryAccount(otherPrimary);
        Recipient foreign = new Recipient();
        foreign.setName("Mallory");
        foreign.setUser(other);

        expectInvalid(() -> service.toSomeoneElseTransfer(foreign, "Primary", "10", primary, savings));

        verifyNoInteractions(primaryAccountDao, primaryTransactionDao);
    }

    @Test
    public void toSomeoneElseRejectsUnknownAccountType() {
        expectInvalid(() -> service.toSomeoneElseTransfer(ownRecipient, "Checking", "10", primary, savings));

        verifyNoInteractions(primaryAccountDao, savingsAccountDao);
    }

    @Test
    public void toSomeoneElseDebitsValidAmountAndRecordsLedger() {
        service.toSomeoneElseTransfer(ownRecipient, "Primary", "40.50", primary, savings);

        assertEquals(new BigDecimal("59.50"), primary.getAccountBalance());
        verify(primaryAccountDao).save(primary);
        ArgumentCaptor<PrimaryTransaction> ledger = ArgumentCaptor.forClass(PrimaryTransaction.class);
        verify(primaryTransactionDao).save(ledger.capture());
        assertEquals(40.50, ledger.getValue().getAmount(), 0.0001);
        assertEquals(new BigDecimal("59.50"), ledger.getValue().getAvailableBalance());
    }

    @Test
    public void betweenAccountsRejectsNegativeAmount() {
        expectInvalid(() -> service.betweenAccountsTransfer("Savings", "Primary", "-500", primary, savings));

        assertEquals(new BigDecimal("100.00"), primary.getAccountBalance());
        assertEquals(new BigDecimal("50.00"), savings.getAccountBalance());
        verifyNoInteractions(primaryAccountDao, savingsAccountDao);
    }

    @Test
    public void betweenAccountsRejectsOverdraft() {
        try {
            service.betweenAccountsTransfer("Primary", "Savings", "100.01", primary, savings);
            fail("expected InsufficientFundsException");
        } catch (InsufficientFundsException expected) {
        }

        assertEquals(new BigDecimal("100.00"), primary.getAccountBalance());
        assertEquals(new BigDecimal("50.00"), savings.getAccountBalance());
        verify(primaryAccountDao, never()).save(any());
    }

    @Test
    public void betweenAccountsRejectsSameAccount() {
        expectInvalid(() -> service.betweenAccountsTransfer("Primary", "Primary", "10", primary, savings));
    }

    @Test
    public void betweenAccountsMovesExactBalance() {
        service.betweenAccountsTransfer("Primary", "Savings", "100", primary, savings);

        assertEquals(0, primary.getAccountBalance().signum());
        assertEquals(new BigDecimal("150.00"), savings.getAccountBalance());
        verify(primaryAccountDao).save(primary);
        verify(savingsAccountDao).save(savings);
        verify(primaryTransactionDao).save(any(PrimaryTransaction.class));
    }

    private static void expectInvalid(Runnable call) {
        try {
            call.run();
            fail("expected InvalidTransferException");
        } catch (InvalidTransferException expected) {
        }
    }
}
