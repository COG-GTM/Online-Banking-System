package com.userfront.dao;

import java.util.List;

import org.springframework.data.repository.CrudRepository;

import com.userfront.domain.Recipient;

public interface RecipientDao extends CrudRepository<Recipient, Long> {
    List<Recipient> findAll();

    List<Recipient> findByUser_Username(String username);

    Recipient findByIdAndUser_Username(Long id, String username);

    void deleteByIdAndUser_Username(Long id, String username);
}
