package com.userfront.service.UserServiceImpl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;

import javax.persistence.EntityManager;

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
import com.userfront.service.InsufficientFundsException;
import com.userfront.service.InvalidTransferException;
import com.userfront.service.UserService;

@RunWith(MockitoJUnitRunner.Silent.class)
public class TransactionServiceImplTest {

    @Mock private UserService userService;
    @Mock private PrimaryTransactionDao primaryTransactionDao;
    @Mock private SavingsTransactionDao savingsTransactionDao;
    @Mock private PrimaryAccountDao primaryAccountDao;
    @Mock private SavingsAccountDao savingsAccountDao;
    @Mock private RecipientDao recipientDao;
    @Mock private EntityManager entityManager;

    @InjectMocks private TransactionServiceImpl service;

    private PrimaryAccount primary;
    private SavingsAccount savings;
    private Recipient recipient;

    @Before
    public void setUp() {
        primary = new PrimaryAccount();
        primary.setId(1L);
        primary.setAccountBalance(new BigDecimal("100.00"));
        savings = new SavingsAccount();
        savings.setId(2L);
        savings.setAccountBalance(new BigDecimal("40.00"));
        recipient = new Recipient();
        recipient.setName("bob");
        when(primaryAccountDao.findByIdForUpdate(1L)).thenReturn(primary);
        when(savingsAccountDao.findByIdForUpdate(2L)).thenReturn(savings);
    }

    @Test
    public void toSomeoneElseDebitsValidAmount() {
        service.toSomeoneElseTransfer(recipient, "Primary", "25.50", primary, savings);

        assertEquals(new BigDecimal("74.50"), primary.getAccountBalance());
        verify(primaryAccountDao).findByIdForUpdate(1L);
        verify(primaryAccountDao).save(primary);
        verify(primaryTransactionDao).save(any(PrimaryTransaction.class));
    }

    @Test
    public void toSomeoneElseChecksTheLockedBalanceNotTheCallersCopy() {
        PrimaryAccount stale = new PrimaryAccount();
        stale.setId(1L);
        stale.setAccountBalance(new BigDecimal("1000.00"));
        primary.setAccountBalance(new BigDecimal("10.00"));

        expectRejected(() -> service.toSomeoneElseTransfer(recipient, "Primary", "80", stale, savings),
                InsufficientFundsException.class);
        assertEquals(new BigDecimal("10.00"), primary.getAccountBalance());
        verify(primaryAccountDao, never()).save(any(PrimaryAccount.class));
    }

    @Test
    public void toSomeoneElseRejectsNegativeAmount() {
        expectRejected(() -> service.toSomeoneElseTransfer(recipient, "Primary", "-1000000", primary, savings),
                InvalidTransferException.class);
        expectRejected(() -> service.toSomeoneElseTransfer(recipient, "Savings", "-5", primary, savings),
                InvalidTransferException.class);
        assertUnchanged();
    }

    @Test
    public void toSomeoneElseRejectsOverdraft() {
        expectRejected(() -> service.toSomeoneElseTransfer(recipient, "Savings", "40.01", primary, savings),
                InsufficientFundsException.class);
        assertUnchanged();
    }

    @Test
    public void toSomeoneElseRejectsMissingRecipientAndUnknownAccount() {
        expectRejected(() -> service.toSomeoneElseTransfer(null, "Primary", "1", primary, savings),
                InvalidTransferException.class);
        expectRejected(() -> service.toSomeoneElseTransfer(recipient, "Checking", "1", primary, savings),
                InvalidTransferException.class);
        assertUnchanged();
    }

    @Test
    public void betweenAccountsMovesValidAmount() throws Exception {
        service.betweenAccountsTransfer("Primary", "Savings", "100", primary, savings);

        assertEquals(new BigDecimal("0.00"), primary.getAccountBalance());
        assertEquals(new BigDecimal("140.00"), savings.getAccountBalance());
    }

    @Test
    public void betweenAccountsRejectsNegativeZeroAndOverdraft() {
        expectRejected(() -> service.betweenAccountsTransfer("Primary", "Savings", "-50", primary, savings),
                InvalidTransferException.class);
        expectRejected(() -> service.betweenAccountsTransfer("Savings", "Primary", "0", primary, savings),
                InvalidTransferException.class);
        expectRejected(() -> service.betweenAccountsTransfer("Savings", "Primary", "40.01", primary, savings),
                InsufficientFundsException.class);
        expectRejected(() -> service.betweenAccountsTransfer("Primary", "Primary", "1", primary, savings),
                InvalidTransferException.class);
        assertUnchanged();
    }

    private void assertUnchanged() {
        assertEquals(new BigDecimal("100.00"), primary.getAccountBalance());
        assertEquals(new BigDecimal("40.00"), savings.getAccountBalance());
        verify(primaryAccountDao, never()).save(any(PrimaryAccount.class));
        verify(savingsAccountDao, never()).save(any(SavingsAccount.class));
        verifyNoMoreInteractions(primaryTransactionDao, savingsTransactionDao);
    }

    private interface Call {
        void run() throws Exception;
    }

    private static void expectRejected(Call call, Class<? extends Exception> type) {
        try {
            call.run();
            fail("expected " + type.getSimpleName());
        } catch (Exception e) {
            if (!type.isInstance(e)) {
                throw new AssertionError("expected " + type.getSimpleName() + " but got " + e, e);
            }
        }
    }
}
