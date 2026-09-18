package com.userfront.service.UserServiceImpl;

import java.util.HashSet;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

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
    private AccountNumberAllocator accountNumberAllocator;

    @Autowired
    private PrimaryAccountDao primaryAccountDao;

    @Autowired
    private SavingsAccountDao savingsAccountDao;

    @Override
    public void run(ApplicationArguments args) {
        Set<Integer> reserved = new HashSet<>();
        for (AccountNumberAllocation allocation : accountNumberAllocationDao.findAll()) {
            reserved.add(allocation.getAccountNumber());
        }

        for (PrimaryAccount primaryAccount : primaryAccountDao.findAll()) {
            reserve(primaryAccount.getAccountNumber(), reserved);
        }

        for (SavingsAccount savingsAccount : savingsAccountDao.findAll()) {
            reserve(savingsAccount.getAccountNumber(), reserved);
        }
    }

    private void reserve(int accountNumber, Set<Integer> reserved) {
        if (!reserved.add(accountNumber)) {
            return;
        }

        try {
            accountNumberAllocator.allocate(accountNumber);
        } catch (DataIntegrityViolationException e) {
            // Another instance reserved the same number; nothing left to do.
        }
    }
}
