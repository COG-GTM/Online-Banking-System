package com.userfront.dao;

import javax.persistence.LockModeType;

import com.userfront.domain.PrimaryAccount;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.repository.CrudRepository;

/**
 * Created by z00382545 on 10/21/16.
 */
public interface PrimaryAccountDao extends CrudRepository<PrimaryAccount,Long> {

    PrimaryAccount findByAccountNumber (int accountNumber);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    PrimaryAccount findForUpdateByAccountNumber (int accountNumber);
}
