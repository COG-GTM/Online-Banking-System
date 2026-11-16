package com.userfront.service.UserServiceImpl;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.security.Principal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.userfront.dao.PrimaryAccountDao;
import com.userfront.dao.SavingsAccountDao;
import com.userfront.domain.PrimaryAccount;
import com.userfront.domain.Recipient;
import com.userfront.domain.SavingsAccount;
import com.userfront.domain.User;
import com.userfront.service.InsufficientFundsException;
import com.userfront.service.UserService;

@RunWith(SpringRunner.class)
@DataJpaTest
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@Import({AccountServiceImpl.class, TransactionServiceImpl.class})
@TestPropertySource(properties = {
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "spring.jpa.properties.hibernate.globally_quoted_identifiers=true",
        "spring.main.allow-circular-references=true"
})
public class ConcurrentBalanceUpdateTest {

    private static final int REQUESTS = 20;

    @MockBean
    private UserService userService;

    @Autowired
    private AccountServiceImpl accountService;

    @Autowired
    private TransactionServiceImpl transactionService;

    @Autowired
    private PrimaryAccountDao primaryAccountDao;

    @Autowired
    private SavingsAccountDao savingsAccountDao;

    private final Principal principal = () -> "mallory";

    private PrimaryAccount primary;
    private SavingsAccount savings;

    @Before
    public void setUp() {
        primary = new PrimaryAccount();
        primary.setAccountBalance(new BigDecimal("100.00"));
        primary = primaryAccountDao.save(primary);
        savings = new SavingsAccount();
        savings.setAccountBalance(new BigDecimal("100.00"));
        savings = savingsAccountDao.save(savings);
        User user = new User();
        user.setPrimaryAccount(primary);
        user.setSavingsAccount(savings);
        when(userService.findByUsername("mallory")).thenReturn(user);
    }

    @Test
    public void parallelWithdrawalsSucceedExactlyUntilBalanceIsZero() throws Exception {
        int succeeded = runInParallel(() -> accountService.withdraw("Primary", new BigDecimal("10"), principal));

        assertEquals(10, succeeded);
        assertBalance("0.00", primaryAccountDao.findById(primary.getId()).get().getAccountBalance());
    }

    @Test
    public void parallelWithdrawalsAndDepositsLoseNoUpdate() throws Exception {
        List<Callable<Void>> tasks = new ArrayList<>();
        for (int i = 0; i < REQUESTS; i++) {
            if (i % 2 == 0) {
                tasks.add(() -> { accountService.withdraw("Savings", new BigDecimal("5"), principal); return null; });
            } else {
                tasks.add(() -> { accountService.deposit("Savings", new BigDecimal("1"), principal); return null; });
            }
        }
        assertEquals(REQUESTS, runAll(tasks));
        assertBalance("60.00", savingsAccountDao.findById(savings.getId()).get().getAccountBalance());
    }

    @Test
    public void parallelBetweenAccountTransfersMoveExactlyTheBalance() throws Exception {
        int succeeded = runInParallel(() -> transactionService.betweenAccountsTransfer("Primary", "Savings", "10", primary, savings));

        assertEquals(10, succeeded);
        assertBalance("0.00", primaryAccountDao.findById(primary.getId()).get().getAccountBalance());
        assertBalance("200.00", savingsAccountDao.findById(savings.getId()).get().getAccountBalance());
    }

    @Test
    public void parallelTransfersToSomeoneElseNeverOverdraw() throws Exception {
        Recipient recipient = new Recipient();
        recipient.setName("bob");
        int succeeded = runInParallel(() -> transactionService.toSomeoneElseTransfer(recipient, "Savings", "10", primary, savings));

        assertEquals(10, succeeded);
        assertBalance("0.00", savingsAccountDao.findById(savings.getId()).get().getAccountBalance());
    }

    private interface Action {
        void run() throws Exception;
    }

    private int runInParallel(Action action) throws Exception {
        List<Callable<Void>> tasks = new ArrayList<>();
        for (int i = 0; i < REQUESTS; i++) {
            tasks.add(() -> { action.run(); return null; });
        }
        return runAll(tasks);
    }

    private int runAll(List<Callable<Void>> tasks) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(tasks.size());
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Boolean>> results = new ArrayList<>();
        for (Callable<Void> task : tasks) {
            results.add(pool.submit(() -> {
                start.await();
                try {
                    task.call();
                    return true;
                } catch (InsufficientFundsException declined) {
                    return false;
                }
            }));
        }
        start.countDown();
        int succeeded = 0;
        for (Future<Boolean> result : results) {
            if (result.get(60, TimeUnit.SECONDS)) {
                succeeded++;
            }
        }
        pool.shutdown();
        return succeeded;
    }

    private static void assertBalance(String expected, BigDecimal actual) {
        assertEquals(expected + " vs " + actual, 0, new BigDecimal(expected).compareTo(actual));
    }
}
