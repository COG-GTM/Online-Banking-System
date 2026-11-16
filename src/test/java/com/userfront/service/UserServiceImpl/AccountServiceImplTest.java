package com.userfront.service.UserServiceImpl;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.security.Principal;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import com.userfront.dao.PrimaryAccountDao;
import com.userfront.dao.SavingsAccountDao;
import com.userfront.domain.PrimaryAccount;
import com.userfront.domain.PrimaryTransaction;
import com.userfront.domain.SavingsAccount;
import com.userfront.domain.SavingsTransaction;
import com.userfront.domain.User;
import com.userfront.service.InsufficientFundsException;
import com.userfront.service.TransactionDeclinedException;
import com.userfront.service.TransactionService;
import com.userfront.service.UserService;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
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

    private final Principal principal = () -> "mallory";

    private PrimaryAccount primaryAccount;
    private SavingsAccount savingsAccount;

    @BeforeEach
    public void setUp() {
        primaryAccount = new PrimaryAccount();
        primaryAccount.setId(1L);
        primaryAccount.setAccountBalance(new BigDecimal("100.00"));
        savingsAccount = new SavingsAccount();
        savingsAccount.setId(2L);
        savingsAccount.setAccountBalance(new BigDecimal("100.00"));
        User user = new User();
        user.setPrimaryAccount(primaryAccount);
        user.setSavingsAccount(savingsAccount);
        when(userService.findByUsername("mallory")).thenReturn(user);
    }

    @Test
    public void withdrawLargerThanBalanceIsDeclinedAndNotRecorded() {
        when(primaryAccountDao.debitIfSufficient(1L, new BigDecimal("1000000.00"))).thenReturn(0);

        try {
            accountService.withdraw("Primary", new BigDecimal("1000000"), principal);
            org.junit.jupiter.api.Assertions.fail("expected InsufficientFundsException");
        } catch (InsufficientFundsException expected) {
        }

        verify(transactionService, never()).savePrimaryWithdrawTransaction(any());
    }

    @Test
    public void savingsWithdrawLargerThanBalanceIsDeclinedAndNotRecorded() {
        when(savingsAccountDao.debitIfSufficient(2L, new BigDecimal("100.01"))).thenReturn(0);

        try {
            accountService.withdraw("Savings", new BigDecimal("100.01"), principal);
            org.junit.jupiter.api.Assertions.fail("expected InsufficientFundsException");
        } catch (InsufficientFundsException expected) {
        }

        verify(transactionService, never()).saveSavingsWithdrawTransaction(any());
    }

    @Test
    public void negativeWithdrawIsRejectedBeforeTouchingTheAccount() {
        try {
            accountService.withdraw("Primary", new BigDecimal("-1000000"), principal);
            org.junit.jupiter.api.Assertions.fail("expected TransactionDeclinedException");
        } catch (TransactionDeclinedException expected) {
        }

        verifyNoMoreInteractions(primaryAccountDao, savingsAccountDao, transactionService);
    }

    @Test
    public void negativeDepositIsRejectedBeforeTouchingTheAccount() {
        try {
            accountService.deposit("Savings", new BigDecimal("-5"), principal);
            org.junit.jupiter.api.Assertions.fail("expected TransactionDeclinedException");
        } catch (TransactionDeclinedException expected) {
        }

        verifyNoMoreInteractions(primaryAccountDao, savingsAccountDao, transactionService);
    }

    @Test
    public void unknownAccountTypeIsRejected() {
        assertThrows(TransactionDeclinedException.class, () -> {
            accountService.withdraw("Checking", new BigDecimal("1"), principal);
        });
    }

    @Test
    public void coveredWithdrawDebitsAtomicallyAndRecordsNewBalance() {
        when(primaryAccountDao.debitIfSufficient(1L, new BigDecimal("40.00"))).thenReturn(1);
        PrimaryAccount reloaded = new PrimaryAccount();
        reloaded.setId(1L);
        reloaded.setAccountBalance(new BigDecimal("60.00"));
        when(primaryAccountDao.findById(1L)).thenReturn(Optional.of(reloaded));

        accountService.withdraw("Primary", new BigDecimal("40"), principal);

        ArgumentCaptor<PrimaryTransaction> captor = ArgumentCaptor.forClass(PrimaryTransaction.class);
        verify(transactionService).savePrimaryWithdrawTransaction(captor.capture());
        assertEquals(40.0, captor.getValue().getAmount(), 0.0);
        assertEquals(new BigDecimal("60.00"), captor.getValue().getAvailableBalance());
        verify(primaryAccountDao, never()).save(any(PrimaryAccount.class));
    }

    @Test
    public void depositCreditsAtomicallyAndRecordsNewBalance() {
        when(savingsAccountDao.credit(2L, new BigDecimal("25.50"))).thenReturn(1);
        SavingsAccount reloaded = new SavingsAccount();
        reloaded.setId(2L);
        reloaded.setAccountBalance(new BigDecimal("125.50"));
        when(savingsAccountDao.findById(2L)).thenReturn(Optional.of(reloaded));

        accountService.deposit("Savings", new BigDecimal("25.5"), principal);

        ArgumentCaptor<SavingsTransaction> captor = ArgumentCaptor.forClass(SavingsTransaction.class);
        verify(transactionService).saveSavingsDepositTransaction(captor.capture());
        assertEquals(new BigDecimal("125.50"), captor.getValue().getAvailableBalance());
        verify(savingsAccountDao, never()).save(any(SavingsAccount.class));
        verify(primaryAccountDao, never()).credit(anyLong(), any());
    }
}
