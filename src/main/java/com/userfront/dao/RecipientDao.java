package com.userfront.dao;

import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.repository.CrudRepository;

import com.userfront.domain.Recipient;

public interface RecipientDao extends CrudRepository<Recipient, Long> {
    List<Recipient> findAll();

    @EntityGraph(attributePaths = "user")
    List<Recipient> findByUserUsername(String username);

    Recipient findByName(String recipientName);

    void deleteByName(String recipientName);
}
