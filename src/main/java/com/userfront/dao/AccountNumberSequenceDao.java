package com.userfront.dao;

import javax.persistence.LockModeType;

import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import com.userfront.domain.AccountNumberSequence;

public interface AccountNumberSequenceDao extends CrudRepository<AccountNumberSequence, String> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from AccountNumberSequence s where s.name = :name")
    AccountNumberSequence findByNameForUpdate(@Param("name") String name);

    @Query("select count(s) from AccountNumberSequence s where s.name = :name")
    long countByName(@Param("name") String name);
}
