package com.userfront.dao;

import javax.persistence.LockModeType;

import com.userfront.domain.PrimaryAccount;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

/**
 * Created by z00382545 on 10/21/16.
 */
public interface PrimaryAccountDao extends CrudRepository<PrimaryAccount,Long> {

    PrimaryAccount findByAccountNumber (int accountNumber);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from PrimaryAccount a where a.id = :id")
    PrimaryAccount findByIdForUpdate(@Param("id") Long id);
}
