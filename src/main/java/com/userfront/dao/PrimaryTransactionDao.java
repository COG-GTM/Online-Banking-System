package com.userfront.dao;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.CrudRepository;

import com.userfront.domain.PrimaryAccount;
import com.userfront.domain.PrimaryTransaction;

public interface PrimaryTransactionDao extends CrudRepository<PrimaryTransaction, Long> {

    List<PrimaryTransaction> findAll();

    Page<PrimaryTransaction> findByPrimaryAccountOrderByDateDescIdDesc(PrimaryAccount primaryAccount, Pageable pageable);
}
