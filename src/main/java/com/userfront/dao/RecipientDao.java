package com.userfront.dao;

import java.util.List;

import org.springframework.data.repository.CrudRepository;

import com.userfront.domain.Recipient;
import com.userfront.domain.User;

public interface RecipientDao extends CrudRepository<Recipient, Long> {
    List<Recipient> findAll();

    List<Recipient> findByUser(User user);

    Recipient findByNameAndUser(String recipientName, User user);

    void deleteByNameAndUser(String recipientName, User user);
}
