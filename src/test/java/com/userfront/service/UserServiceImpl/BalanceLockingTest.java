package com.userfront.service.UserServiceImpl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.security.Principal;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.transaction.annotation.Transactional;

import com.userfront.dao.PrimaryAccountDao;
import com.userfront.dao.PrimaryTransactionDao;
import com.userfront.dao.SavingsAccountDao;
import com.userfront.dao.SavingsTransactionDao;
import com.userfront.domain.PrimaryAccount;
import com.userfront.domain.Recipient;
import com.userfront.domain.SavingsAccount;
import com.userfront.domain.User;
import com.userfront.service.TransactionService;
import com.userfront.service.UserService;

@RunWith(MockitoJUnitRunner.Silent.class)
public class BalanceLockingTest {

    @Mock private UserService userService;
    @Mock private TransactionService transactionService;
    @Mock private PrimaryAccountDao primaryAccountDao;
    @Mock private SavingsAccountDao savingsAccountDao;
    @Mock private PrimaryTransactionDao primaryTransactionDao;
    @Mock private SavingsTransactionDao savingsTransactionDao;
    @Mock private AccountLocks accountLocks;

    @InjectMocks private AccountServiceImpl accountService;
    @InjectMocks private TransactionServiceImpl transactionServiceImpl;

    private final Principal principal = () -> "alice";
    private PrimaryAccount stalePrimary;
    private SavingsAccount staleSavings;
    private PrimaryAccount lockedPrimary;
    private SavingsAccount lockedSavings;

    @Before
    public void setUp() {
        stalePrimary = primary(1L, "100");
        staleSavings = savings(2L, "100");
        lockedPrimary = primary(1L, "150");
        lockedSavings = savings(2L, "150");

        User user = new User();
        user.setUsername("alice");
        user.setPrimaryAccount(stalePrimary);
        user.setSavingsAccount(staleSavings);
        when(userService.findByUsername("alice")).thenReturn(user);
        when(accountLocks.lockPrimary(1L)).thenReturn(lockedPrimary);
        when(accountLocks.lockSavings(2L)).thenReturn(lockedSavings);
    }

    @Test
    public void depositAppliesToLockedLatestBalance() {
        accountService.deposit("Primary", 10, principal);

        verify(primaryAccountDao).save(lockedPrimary);
        verify(primaryAccountDao, never()).save(stalePrimary);
        assertEquals(0, new BigDecimal("160").compareTo(lockedPrimary.getAccountBalance()));
        assertEquals(0, new BigDecimal("100").compareTo(stalePrimary.getAccountBalance()));
    }

    @Test
    public void withdrawAppliesToLockedLatestBalance() {
        accountService.withdraw("Savings", 30, principal);

        verify(savingsAccountDao).save(lockedSavings);
        verify(savingsAccountDao, never()).save(staleSavings);
        assertEquals(0, new BigDecimal("120").compareTo(lockedSavings.getAccountBalance()));
    }

    @Test
    public void betweenAccountsTransferLocksPrimaryThenSavings() throws Exception {
        transactionServiceImpl.betweenAccountsTransfer("Savings", "Primary", "50", stalePrimary, staleSavings);

        InOrder order = inOrder(accountLocks);
        order.verify(accountLocks).lockPrimary(1L);
        order.verify(accountLocks).lockSavings(2L);
        assertEquals(0, new BigDecimal("200").compareTo(lockedPrimary.getAccountBalance()));
        assertEquals(0, new BigDecimal("100").compareTo(lockedSavings.getAccountBalance()));
        verify(primaryAccountDao).save(lockedPrimary);
        verify(savingsAccountDao).save(lockedSavings);
    }

    @Test
    public void invalidTransferDirectionFailsBeforeLocking() {
        try {
            transactionServiceImpl.betweenAccountsTransfer("Primary", "Primary", "50", stalePrimary, staleSavings);
            fail("expected Invalid Transfer");
        } catch (Exception expected) {
            assertEquals("Invalid Transfer", expected.getMessage());
        }
        verify(accountLocks, never()).lockPrimary(anyLong());
        verify(accountLocks, never()).lockSavings(anyLong());
        verify(primaryAccountDao, never()).save(any(PrimaryAccount.class));
    }

    @Test
    public void toSomeoneElseTransferDebitsLockedAccount() {
        Recipient recipient = new Recipient();
        recipient.setName("bob");

        transactionServiceImpl.toSomeoneElseTransfer(recipient, "Primary", "40", stalePrimary, staleSavings);

        verify(primaryAccountDao).save(lockedPrimary);
        assertEquals(0, new BigDecimal("110").compareTo(lockedPrimary.getAccountBalance()));
    }

    @Test
    public void balanceMutationsRunInTransactions() throws Exception {
        assertTransactional(AccountServiceImpl.class.getMethod("deposit", String.class, double.class, Principal.class));
        assertTransactional(AccountServiceImpl.class.getMethod("withdraw", String.class, double.class, Principal.class));
        assertTransactional(TransactionServiceImpl.class.getMethod("toSomeoneElseTransfer",
                Recipient.class, String.class, String.class, PrimaryAccount.class, SavingsAccount.class));
        Transactional transfer = assertTransactional(TransactionServiceImpl.class.getMethod("betweenAccountsTransfer",
                String.class, String.class, String.class, PrimaryAccount.class, SavingsAccount.class));
        assertTrue(java.util.Arrays.asList(transfer.rollbackFor()).contains(Exception.class));
    }

    @Test
    public void accountsAreVersioned() throws Exception {
        assertNotNull(PrimaryAccount.class.getDeclaredField("version").getAnnotation(javax.persistence.Version.class));
        assertNotNull(SavingsAccount.class.getDeclaredField("version").getAnnotation(javax.persistence.Version.class));
    }

    private static Transactional assertTransactional(Method method) {
        Transactional tx = method.getAnnotation(Transactional.class);
        assertNotNull(method.getName() + " must be @Transactional", tx);
        return tx;
    }

    private static PrimaryAccount primary(Long id, String balance) {
        PrimaryAccount account = new PrimaryAccount();
        account.setId(id);
        account.setAccountBalance(new BigDecimal(balance));
        return account;
    }

    private static SavingsAccount savings(Long id, String balance) {
        SavingsAccount account = new SavingsAccount();
        account.setId(id);
        account.setAccountBalance(new BigDecimal(balance));
        return account;
    }
}
