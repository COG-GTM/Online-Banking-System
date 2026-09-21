package com.userfront.dao;

import java.util.List;
import java.util.Optional;

import org.springframework.data.repository.CrudRepository;

import com.userfront.domain.Recipient;
import com.userfront.domain.User;

public interface RecipientDao extends CrudRepository<Recipient, Long> {
    List<Recipient> findByUser(User user);

    Optional<Recipient> findByIdAndUser(Long id, User user);

    Optional<Recipient> findByNameAndUser(String recipientName, User user);

    long deleteByIdAndUser(Long id, User user);
}
