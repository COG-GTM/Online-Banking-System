package com.userfront.service.UserServiceImpl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.userfront.dao.AccountNumberAllocationDao;
import com.userfront.dao.PrimaryAccountDao;
import com.userfront.dao.SavingsAccountDao;
import com.userfront.domain.AccountNumberAllocation;
import com.userfront.domain.PrimaryAccount;
import com.userfront.domain.SavingsAccount;

/**
 * Registers the numbers of accounts that were created before the allocation
 * table existed, so that they can never be handed out again.
 */
@Component
public class AccountNumberBackfill implements ApplicationRunner {

    @Autowired
    private AccountNumberAllocationDao accountNumberAllocationDao;

    @Autowired
    private PrimaryAccountDao primaryAccountDao;

    @Autowired
    private SavingsAccountDao savingsAccountDao;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        for (PrimaryAccount primaryAccount : primaryAccountDao.findAll()) {
            reserve(primaryAccount.getAccountNumber());
        }

        for (SavingsAccount savingsAccount : savingsAccountDao.findAll()) {
            reserve(savingsAccount.getAccountNumber());
        }
    }

    private void reserve(int accountNumber) {
        if (!accountNumberAllocationDao.existsById(accountNumber)) {
            accountNumberAllocationDao.save(new AccountNumberAllocation(accountNumber));
        }
    }
}
