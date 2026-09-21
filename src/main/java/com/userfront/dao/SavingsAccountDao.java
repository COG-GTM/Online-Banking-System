package com.userfront.dao;

import java.util.Optional;

import com.userfront.domain.SavingsAccount;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.repository.CrudRepository;

/**
 * Created by z00382545 on 10/21/16.
 */
public interface SavingsAccountDao extends CrudRepository<SavingsAccount, Long> {

    SavingsAccount findByAccountNumber (int accountNumber);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<SavingsAccount> findWithLockById(Long id);
}
