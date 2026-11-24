package com.userfront.dao;

import org.springframework.data.repository.CrudRepository;

import com.userfront.domain.PrimaryTransaction;

public interface PrimaryTransactionDao extends CrudRepository<PrimaryTransaction, Long> {
}
