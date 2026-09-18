package com.userfront.service.UserServiceImpl;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.userfront.domain.AccountNumber;

/**
 * Claims an account number in the shared account number table. The insert runs in its own
 * transaction so that a losing race only rolls back the claim, leaving the caller's transaction
 * usable for another attempt.
 */
@Repository
public class AccountNumberReserver {

    @PersistenceContext
    private EntityManager entityManager;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void reserve(int accountNumber) {
        entityManager.persist(new AccountNumber(accountNumber));
        entityManager.flush();
    }

}
