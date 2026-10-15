package com.userfront.service.UserServiceImpl;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.userfront.dao.PrimaryAccountDao;
import com.userfront.dao.SavingsAccountDao;
import com.userfront.domain.AccountNumberReservation;

/**
 * Reserves account numbers in one table shared by primary and savings accounts.
 * The reservation commits in its own transaction, so a number claimed concurrently
 * fails on the primary key and the caller can retry without poisoning the signup.
 */
@Component
public class AccountNumberAllocator {

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private PrimaryAccountDao primaryAccountDao;

    @Autowired
    private SavingsAccountDao savingsAccountDao;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean reserve(int accountNumber) {
        if (entityManager.find(AccountNumberReservation.class, accountNumber) != null
                || primaryAccountDao.findByAccountNumber(accountNumber) != null
                || savingsAccountDao.findByAccountNumber(accountNumber) != null) {
            return false;
        }
        entityManager.persist(new AccountNumberReservation(accountNumber));
        entityManager.flush();
        return true;
    }
}
