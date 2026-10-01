package com.userfront.dao;

import java.util.List;

import org.springframework.data.repository.CrudRepository;

import com.userfront.domain.Recipient;
import com.userfront.domain.User;

public interface RecipientDao extends CrudRepository<Recipient, Long> {
    List<Recipient> findAll();

    Recipient findByName(String recipientName);

    Recipient findByNameAndUser(String recipientName, User user);

    void deleteByName(String recipientName);
}
