package com.userfront.dao;

import com.userfront.domain.SavingsAccount;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

/**
 * Created by z00382545 on 10/21/16.
 */
public interface SavingsAccountDao extends CrudRepository<SavingsAccount, Long> {

    SavingsAccount findByAccountNumber (int accountNumber);

    @Query("select max(a.accountNumber) from SavingsAccount a")
    Integer findMaxAccountNumber();

    @Query("select count(a) from SavingsAccount a where a.accountNumber = :accountNumber")
    long countByAccountNumber(@Param("accountNumber") int accountNumber);
}
