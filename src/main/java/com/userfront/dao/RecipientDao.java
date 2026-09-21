package com.userfront.dao;

import java.util.List;

import org.springframework.data.repository.CrudRepository;

import com.userfront.domain.Recipient;

public interface RecipientDao extends CrudRepository<Recipient, Long> {
    List<Recipient> findAll();

    List<Recipient> findByUserUsername(String username);

    Recipient findByNameAndUserUsername(String recipientName, String username);

    void deleteByNameAndUserUsername(String recipientName, String username);
}
