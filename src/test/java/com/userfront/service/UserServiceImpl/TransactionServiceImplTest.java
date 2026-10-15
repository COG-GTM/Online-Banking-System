package com.userfront.service.UserServiceImpl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.userfront.dao.PrimaryAccountDao;
import com.userfront.dao.PrimaryTransactionDao;
import com.userfront.dao.SavingsAccountDao;
import com.userfront.dao.SavingsTransactionDao;
import com.userfront.domain.PrimaryAccount;
import com.userfront.domain.Recipient;
import com.userfront.domain.SavingsAccount;
import com.userfront.validation.InsufficientFundsException;
import com.userfront.validation.InvalidAmountException;

public class TransactionServiceImplTest {

    private final List<Object> saved = new ArrayList<>();
    private TransactionServiceImpl service;
    private PrimaryAccount primary;
    private SavingsAccount savings;

    @Before
    public void setUp() {
        service = new TransactionServiceImpl();
        ReflectionTestUtils.setField(service, "primaryAccountDao", recordingDao(PrimaryAccountDao.class));
        ReflectionTestUtils.setField(service, "savingsAccountDao", recordingDao(SavingsAccountDao.class));
        ReflectionTestUtils.setField(service, "primaryTransactionDao", recordingDao(PrimaryTransactionDao.class));
        ReflectionTestUtils.setField(service, "savingsTransactionDao", recordingDao(SavingsTransactionDao.class));
        ReflectionTestUtils.setField(service, "accountLocks", passThroughLocks());

        primary = new PrimaryAccount();
        primary.setAccountBalance(new BigDecimal("100.00"));
        savings = new SavingsAccount();
        savings.setAccountBalance(new BigDecimal("50.00"));
    }

    @Test
    public void betweenAccountsTransferMovesPositiveAmount() throws Exception {
        service.betweenAccountsTransfer("Primary", "Savings", new BigDecimal("25.50"), primary, savings);

        assertEquals(new BigDecimal("74.50"), primary.getAccountBalance());
        assertEquals(new BigDecimal("75.50"), savings.getAccountBalance());
    }

    @Test
    public void betweenAccountsTransferRejectsNegativeAmountWithoutTouchingBalances() throws Exception {
        for (String direction : new String[] {"Primary", "Savings"}) {
            String other = direction.equals("Primary") ? "Savings" : "Primary";
            try {
                service.betweenAccountsTransfer(direction, other, new BigDecimal("-1000"), primary, savings);
                fail("negative transfer accepted");
            } catch (InvalidAmountException expected) {
            }
        }
        assertUnchanged();
    }

    @Test
    public void betweenAccountsTransferRejectsZeroAndFractionalCents() throws Exception {
        for (BigDecimal amount : new BigDecimal[] {BigDecimal.ZERO, new BigDecimal("0.001"), null}) {
            try {
                service.betweenAccountsTransfer("Primary", "Savings", amount, primary, savings);
                fail("accepted " + amount);
            } catch (InvalidAmountException expected) {
            }
        }
        assertUnchanged();
    }

    @Test
    public void toSomeoneElseTransferRejectsNegativeAmount() {
        Recipient recipient = new Recipient();
        recipient.setName("bob");
        for (String accountType : new String[] {"Primary", "Savings"}) {
            try {
                service.toSomeoneElseTransfer(recipient, accountType, new BigDecimal("-10"), primary, savings);
                fail("negative transfer accepted");
            } catch (InvalidAmountException expected) {
            }
        }
        assertUnchanged();
    }

    @Test
    public void toSomeoneElseTransferDebitsPositiveAmount() {
        Recipient recipient = new Recipient();
        recipient.setName("bob");
        service.toSomeoneElseTransfer(recipient, "Savings", new BigDecimal("20"), primary, savings);

        assertEquals(new BigDecimal("30.00"), savings.getAccountBalance());
        assertEquals(new BigDecimal("100.00"), primary.getAccountBalance());
    }

    @Test
    public void betweenAccountsTransferRejectsOverdraftInBothDirections() throws Exception {
        try {
            service.betweenAccountsTransfer("Primary", "Savings", new BigDecimal("100.01"), primary, savings);
            fail("primary overdraft accepted");
        } catch (InsufficientFundsException expected) {
        }
        try {
            service.betweenAccountsTransfer("Savings", "Primary", new BigDecimal("50.01"), primary, savings);
            fail("savings overdraft accepted");
        } catch (InsufficientFundsException expected) {
        }
        assertUnchanged();
    }

    @Test
    public void betweenAccountsTransferAllowsFullBalance() throws Exception {
        service.betweenAccountsTransfer("Savings", "Primary", new BigDecimal("50"), primary, savings);

        assertEquals(new BigDecimal("150.00"), primary.getAccountBalance());
        assertEquals(new BigDecimal("0.00"), savings.getAccountBalance());
    }

    @Test
    public void toSomeoneElseTransferRejectsOverdraft() {
        Recipient recipient = new Recipient();
        recipient.setName("bob");
        try {
            service.toSomeoneElseTransfer(recipient, "Primary", new BigDecimal("500000"), primary, savings);
            fail("primary overdraft accepted");
        } catch (InsufficientFundsException expected) {
        }
        try {
            service.toSomeoneElseTransfer(recipient, "Savings", new BigDecimal("50.01"), primary, savings);
            fail("savings overdraft accepted");
        } catch (InsufficientFundsException expected) {
        }
        assertUnchanged();
    }

    private void assertUnchanged() {
        assertEquals(new BigDecimal("100.00"), primary.getAccountBalance());
        assertEquals(new BigDecimal("50.00"), savings.getAccountBalance());
        assertEquals(0, saved.size());
    }

    private <T> T recordingDao(Class<T> type) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] {type}, (proxy, method, args) -> {
            if (method.getName().equals("save")) {
                saved.add(args[0]);
                return args[0];
            }
            if (method.getName().equals("toString")) {
                return type.getSimpleName();
            }
            throw new UnsupportedOperationException(method.getName());
        }));
    }

    private static AccountLocks passThroughLocks() {
        return new AccountLocks() {
            @Override
            public PrimaryAccount lock(PrimaryAccount account) {
                return account;
            }

            @Override
            public SavingsAccount lock(SavingsAccount account) {
                return account;
            }
        };
    }
}
