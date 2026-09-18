package com.userfront.service.UserServiceImpl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.userfront.dao.AccountNumberAllocationDao;
import com.userfront.domain.AccountNumberAllocation;

/**
 * Inserts a single account number reservation in its own transaction so that a
 * duplicate key does not roll back the caller's transaction.
 */
@Service
public class AccountNumberAllocator {

    @Autowired
    private AccountNumberAllocationDao accountNumberAllocationDao;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void allocate(int accountNumber) {
        accountNumberAllocationDao.saveAndFlush(new AccountNumberAllocation(accountNumber));
    }
}
