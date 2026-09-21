package com.userfront.dao;

import java.util.List;
import java.util.Optional;

import org.springframework.data.repository.CrudRepository;

import com.userfront.domain.Recipient;

public interface RecipientDao extends CrudRepository<Recipient, Long> {
    List<Recipient> findByUserUsername(String username);

    Optional<Recipient> findByNameAndUserUsername(String recipientName, String username);

    void deleteByNameAndUserUsername(String recipientName, String username);
}
