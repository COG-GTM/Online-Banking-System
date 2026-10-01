package com.userfront.service.UserServiceImpl;

import javax.persistence.EntityManager;
import javax.persistence.LockModeType;
import javax.persistence.PersistenceContext;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.userfront.domain.PrimaryAccount;
import com.userfront.domain.SavingsAccount;

/**
 * Re-reads an account row with SELECT ... FOR UPDATE inside the caller's transaction, so the
 * balance used for a read-modify-write is current and no other transaction can change it until commit.
 * Callers that lock both accounts of a user must lock the primary account first.
 */
@Component
public class AccountLocker {

    @PersistenceContext
    private EntityManager entityManager;

    @Transactional(propagation = Propagation.MANDATORY)
    public PrimaryAccount lockPrimary(PrimaryAccount account) {
        return lock(PrimaryAccount.class, account.getId());
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public SavingsAccount lockSavings(SavingsAccount account) {
        return lock(SavingsAccount.class, account.getId());
    }

    private <T> T lock(Class<T> type, Long id) {
        T managed = entityManager.find(type, id);
        if (managed == null) {
            throw new IllegalStateException(type.getSimpleName() + " " + id + " not found");
        }
        entityManager.refresh(managed, LockModeType.PESSIMISTIC_WRITE);
        return managed;
    }
}
