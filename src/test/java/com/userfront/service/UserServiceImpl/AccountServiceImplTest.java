package com.userfront.service.UserServiceImpl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;

import com.userfront.dao.PrimaryAccountDao;
import com.userfront.dao.SavingsAccountDao;
import com.userfront.domain.PrimaryAccount;
import com.userfront.domain.SavingsAccount;
import com.userfront.service.TransactionService;
import com.userfront.service.UserService;

@DataJpaTest
@Import(AccountServiceImpl.class)
class AccountServiceImplTest {

    @Autowired
    private AccountServiceImpl accountService;

    @Autowired
    private PrimaryAccountDao primaryAccountDao;

    @Autowired
    private SavingsAccountDao savingsAccountDao;

    @MockBean
    private UserService userService;

    @MockBean
    private TransactionService transactionService;

    @Test
    void createsUniqueAccountNumbersWhenAccountsAlreadyExistFromBeforeRestart() {
        PrimaryAccount existingPrimary = new PrimaryAccount();
        existingPrimary.setAccountBalance(BigDecimal.ZERO);
        existingPrimary.setAccountNumber(11223146);
        primaryAccountDao.save(existingPrimary);

        SavingsAccount existingSavings = new SavingsAccount();
        existingSavings.setAccountBalance(BigDecimal.ZERO);
        existingSavings.setAccountNumber(11223147);
        savingsAccountDao.save(existingSavings);

        PrimaryAccount primary = accountService.createPrimaryAccount();
        SavingsAccount savings = accountService.createSavingsAccount();

        assertEquals(11223148, primary.getAccountNumber());
        assertEquals(11223149, savings.getAccountNumber());
        assertNotEquals(existingPrimary.getId(), primary.getId());
        assertNotEquals(existingSavings.getId(), savings.getId());
    }
}
