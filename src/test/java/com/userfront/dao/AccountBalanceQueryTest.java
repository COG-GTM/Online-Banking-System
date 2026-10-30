package com.userfront.dao;

import static org.junit.Assert.assertEquals;

import java.math.BigDecimal;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import com.userfront.domain.PrimaryAccount;
import com.userfront.domain.SavingsAccount;

@RunWith(SpringRunner.class)
@DataJpaTest
@TestPropertySource(properties = "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect")
public class AccountBalanceQueryTest {

    @Autowired
    private PrimaryAccountDao primaryAccountDao;

    @Autowired
    private SavingsAccountDao savingsAccountDao;

    @Test
    public void primaryDebitOnlyAppliesWhenBalanceCoversIt() {
        PrimaryAccount account = new PrimaryAccount();
        account.setAccountNumber(1);
        account.setAccountBalance(new BigDecimal("100.00"));
        Long id = primaryAccountDao.save(account).getId();

        assertEquals(0, primaryAccountDao.debitIfSufficient(id, new BigDecimal("100.01")));
        assertEquals(new BigDecimal("100.00"), primaryAccountDao.findById(id).get().getAccountBalance());

        assertEquals(1, primaryAccountDao.debitIfSufficient(id, new BigDecimal("100.00")));
        assertEquals(0, primaryAccountDao.findById(id).get().getAccountBalance().signum());

        assertEquals(0, primaryAccountDao.debitIfSufficient(id, new BigDecimal("0.01")));
    }

    @Test
    public void savingsCreditAndDebit() {
        SavingsAccount account = new SavingsAccount();
        account.setAccountNumber(2);
        account.setAccountBalance(new BigDecimal("0.00"));
        Long id = savingsAccountDao.save(account).getId();

        assertEquals(0, savingsAccountDao.debitIfSufficient(id, new BigDecimal("1.00")));
        assertEquals(1, savingsAccountDao.credit(id, new BigDecimal("25.50")));
        assertEquals(1, savingsAccountDao.debitIfSufficient(id, new BigDecimal("25.50")));
        assertEquals(0, savingsAccountDao.findById(id).get().getAccountBalance().signum());
    }
}
