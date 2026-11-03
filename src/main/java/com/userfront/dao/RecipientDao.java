package com.userfront.dao;

import java.util.List;

import org.springframework.data.repository.CrudRepository;

import com.userfront.domain.Recipient;

public interface RecipientDao extends CrudRepository<Recipient, Long> {
    List<Recipient> findAll();

    Recipient findByNameAndUser_Username(String name, String username);

    Recipient findByIdAndUser_Username(Long id, String username);

    void deleteByNameAndUser_Username(String name, String username);
}
