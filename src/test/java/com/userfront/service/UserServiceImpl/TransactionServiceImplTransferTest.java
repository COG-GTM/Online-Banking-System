package com.userfront.service.UserServiceImpl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.security.access.AccessDeniedException;

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

        verifyNoInteractions(primaryAccountDao, primaryTransactionDao, savingsAccountDao, savingsTransactionDao);
    }

    @Test
    public void toSomeoneElseRejectsWhenConditionalDebitFindsInsufficientFunds() {
        when(savingsAccountDao.debitIfCovered(2L, new BigDecimal("50.01"))).thenReturn(0);

        try {
            service.toSomeoneElseTransfer(ownRecipient, "Savings", "50.01", primary, savings);
            fail("expected InsufficientFundsException");
        } catch (InsufficientFundsException expected) {
        }

        verifyNoInteractions(savingsTransactionDao);
    }

    @Test
    public void toSomeoneElseRejectsMissingRecipientBeforeMutating() {
        expectInvalid(() -> service.toSomeoneElseTransfer(null, "Primary", "10", primary, savings));

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
        BigDecimal value = new BigDecimal("40.50");
        when(primaryAccountDao.debitIfCovered(1L, value)).thenReturn(1);
        when(primaryAccountDao.findBalanceById(1L)).thenReturn(new BigDecimal("59.50"));

        service.toSomeoneElseTransfer(ownRecipient, "Primary", "40.50", primary, savings);

        verify(primaryAccountDao, never()).save(any());
        ArgumentCaptor<PrimaryTransaction> ledger = ArgumentCaptor.forClass(PrimaryTransaction.class);
        verify(primaryTransactionDao).save(ledger.capture());
        assertEquals(40.50, ledger.getValue().getAmount(), 0.0001);
        assertEquals(new BigDecimal("59.50"), ledger.getValue().getAvailableBalance());
    }

    @Test
    public void betweenAccountsRejectsNegativeAmount() {
        expectInvalid(() -> service.betweenAccountsTransfer("Savings", "Primary", "-500", primary, savings));

        verifyNoInteractions(primaryAccountDao, savingsAccountDao);
    }

    @Test
    public void betweenAccountsRejectsOverdraftWithoutCrediting() {
        when(primaryAccountDao.debitIfCovered(1L, new BigDecimal("100.01"))).thenReturn(0);

        try {
            service.betweenAccountsTransfer("Primary", "Savings", "100.01", primary, savings);
            fail("expected InsufficientFundsException");
        } catch (InsufficientFundsException expected) {
        }

        verifyNoInteractions(savingsAccountDao, primaryTransactionDao);
    }

    @Test
    public void betweenAccountsRejectsSameAccount() {
        expectInvalid(() -> service.betweenAccountsTransfer("Primary", "Primary", "10", primary, savings));
    }

    @Test
    public void betweenAccountsMovesFundsAndRecordsLedger() {
        BigDecimal value = new BigDecimal("30");
        when(savingsAccountDao.debitIfCovered(2L, value)).thenReturn(1);
        when(savingsAccountDao.findBalanceById(2L)).thenReturn(new BigDecimal("20.00"));

        service.betweenAccountsTransfer("Savings", "Primary", "30", primary, savings);

        verify(primaryAccountDao).credit(1L, value);
        ArgumentCaptor<SavingsTransaction> ledger = ArgumentCaptor.forClass(SavingsTransaction.class);
        verify(savingsTransactionDao).save(ledger.capture());
        assertEquals(new BigDecimal("20.00"), ledger.getValue().getAvailableBalance());
    }

    @Test
    public void findRecipientByNameIsScopedToCaller() {
        when(recipientDao.findByNameAndUserUsername("Bob", "alice")).thenReturn(Collections.singletonList(ownRecipient));
        when(recipientDao.findByNameAndUserUsername("Bob", "mallory")).thenReturn(Collections.emptyList());

        assertSame(ownRecipient, service.findRecipientByName("Bob", "alice"));
        assertNull(service.findRecipientByName("Bob", "mallory"));
    }

    @Test
    public void findRecipientByNameRejectsAmbiguousPayee() {
        when(recipientDao.findByNameAndUserUsername("Sam", "alice")).thenReturn(Arrays.asList(new Recipient(), new Recipient()));

        expectInvalid(() -> service.findRecipientByName("Sam", "alice"));
    }

    @Test
    public void saveRecipientRejectsOverwritingAnotherUsersRecipient() {
        User alice = new User();
        alice.setUsername("alice");
        Recipient tampered = new Recipient();
        tampered.setId(42L);
        when(recipientDao.findByIdAndUserUsername(42L, "alice")).thenReturn(null);

        try {
            service.saveRecipient(tampered, alice);
            fail("expected AccessDeniedException");
        } catch (AccessDeniedException expected) {
        }

        verify(recipientDao, never()).save(any());
    }

    @Test
    public void saveRecipientUpdatesOwnRecipientAndCreatesNewOnes() {
        User alice = new User();
        alice.setUsername("alice");
        Recipient existing = new Recipient();
        existing.setId(7L);
        when(recipientDao.findByIdAndUserUsername(7L, "alice")).thenReturn(existing);
        Recipient fresh = new Recipient();

        service.saveRecipient(existing, alice);
        service.saveRecipient(fresh, alice);

        verify(recipientDao).save(existing);
        verify(recipientDao).save(fresh);
        assertSame(alice, fresh.getUser());
    }

    @Test
    public void deleteRecipientIsScopedToCaller() {
        service.deleteRecipientByName("Bob", "mallory");

        verify(recipientDao).deleteByNameAndUserUsername("Bob", "mallory");
    }

    private static void expectInvalid(Runnable call) {
        try {
            call.run();
            fail("expected InvalidTransferException");
        } catch (InvalidTransferException expected) {
        }
    }
}
