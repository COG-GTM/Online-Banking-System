package com.userfront.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.userfront.dao.AccountNumberSequenceDao;
import com.userfront.dao.PrimaryAccountDao;
import com.userfront.dao.SavingsAccountDao;
import com.userfront.domain.PrimaryAccount;
import com.userfront.domain.SavingsAccount;
import com.userfront.service.UserServiceImpl.AccountServiceImpl;

@RunWith(SpringRunner.class)
@DataJpaTest
@TestPropertySource(properties = "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect")
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@Import({AccountNumberGenerator.class, AccountServiceImpl.class})
public class AccountNumberGeneratorTest {

    @Autowired
    private AccountService accountService;

    @Autowired
    private AccountNumberGenerator generator;

    @Autowired
    private AccountNumberSequenceDao sequenceDao;

    @Autowired
    private PrimaryAccountDao primaryAccountDao;

    @Autowired
    private SavingsAccountDao savingsAccountDao;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @MockBean
    private UserService userService;

    @MockBean
    private TransactionService transactionService;

    @After
    public void cleanUp() {
        primaryAccountDao.deleteAll();
        savingsAccountDao.deleteAll();
        sequenceDao.deleteAll();
    }

    @Test
    public void createdAccountsAreSavedWithDistinctNumbers() {
        PrimaryAccount primary = accountService.createPrimaryAccount();
        SavingsAccount savings = accountService.createSavingsAccount();

        assertNotNull(primary.getId());
        assertNotNull(savings.getId());
        assertEquals(AccountNumberGenerator.FIRST_ACCOUNT_NUMBER, primary.getAccountNumber());
        assertEquals(AccountNumberGenerator.FIRST_ACCOUNT_NUMBER + 1, savings.getAccountNumber());
    }

    @Test
    public void numbersContinueAfterRestart() {
        int before = accountService.createPrimaryAccount().getAccountNumber();
        accountService.createSavingsAccount();

        AccountNumberGenerator afterRestart = new AccountNumberGenerator(
                sequenceDao, primaryAccountDao, savingsAccountDao, transactionManager);

        assertEquals(before + 2, afterRestart.nextAccountNumber());
    }

    @Test
    public void existingDatabaseIsSeededAboveTheHighestNumberInUse() {
        primaryAccountDao.save(account(new PrimaryAccount(), 11223146));
        savingsAccountDao.save(account(new SavingsAccount(), 11223147));
        primaryAccountDao.save(account(new PrimaryAccount(), 11223150));

        assertEquals(11223151, generator.nextAccountNumber());
        assertEquals(11223152, accountService.createSavingsAccount().getAccountNumber());
    }

    @Test
    public void concurrentSignupsNeverShareANumber() throws Exception {
        int threads = 8;
        int perThread = 25;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<List<Integer>>> futures = new ArrayList<>();
        for (int t = 0; t < threads; t++) {
            final boolean primary = t % 2 == 0;
            futures.add(pool.submit((Callable<List<Integer>>) () -> {
                start.await();
                List<Integer> numbers = new ArrayList<>();
                for (int i = 0; i < perThread; i++) {
                    numbers.add(primary
                            ? accountService.createPrimaryAccount().getAccountNumber()
                            : accountService.createSavingsAccount().getAccountNumber());
                }
                return numbers;
            }));
        }
        start.countDown();

        Set<Integer> all = new HashSet<>();
        for (Future<List<Integer>> future : futures) {
            all.addAll(future.get(60, TimeUnit.SECONDS));
        }
        pool.shutdown();

        assertEquals(threads * perThread, all.size());
        assertEquals(threads * perThread, primaryAccountDao.count() + savingsAccountDao.count());
    }

    @Test(expected = DataIntegrityViolationException.class)
    public void databaseRejectsDuplicateAccountNumbers() {
        primaryAccountDao.save(account(new PrimaryAccount(), 11223146));
        primaryAccountDao.save(account(new PrimaryAccount(), 11223146));
    }

    private static PrimaryAccount account(PrimaryAccount account, int number) {
        account.setAccountNumber(number);
        account.setAccountBalance(BigDecimal.ZERO);
        return account;
    }

    private static SavingsAccount account(SavingsAccount account, int number) {
        account.setAccountNumber(number);
        account.setAccountBalance(BigDecimal.ZERO);
        return account;
    }
}
