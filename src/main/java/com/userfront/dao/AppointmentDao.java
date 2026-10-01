package com.userfront.dao;

import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import com.userfront.domain.Appointment;

public interface AppointmentDao extends CrudRepository<Appointment, Long> {

    @Query("select distinct a from Appointment a left join fetch a.user u left join fetch u.userRoles ur left join fetch ur.role left join fetch u.primaryAccount left join fetch u.savingsAccount")
    List<Appointment> findAll();
}
