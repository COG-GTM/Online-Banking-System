package com.userfront.dao;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import com.userfront.domain.Appointment;

public interface AppointmentDao extends CrudRepository<Appointment, Long> {

    @Query(value = "select a from Appointment a"
            + " left join fetch a.user u"
            + " left join fetch u.primaryAccount"
            + " left join fetch u.savingsAccount",
            countQuery = "select count(a) from Appointment a")
    Page<Appointment> findAllWithUser(Pageable pageable);
}
