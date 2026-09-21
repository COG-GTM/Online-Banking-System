package com.userfront.dao;

import java.util.Optional;

import com.userfront.domain.PrimaryAccount;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.repository.CrudRepository;

/**
 * Created by z00382545 on 10/21/16.
 */
public interface PrimaryAccountDao extends CrudRepository<PrimaryAccount,Long> {

    PrimaryAccount findByAccountNumber (int accountNumber);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<PrimaryAccount> findWithLockById(Long id);
}
