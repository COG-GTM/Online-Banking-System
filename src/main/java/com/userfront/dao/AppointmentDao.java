package com.userfront.dao;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import com.userfront.domain.Appointment;
import com.userfront.resource.dto.AppointmentSummary;

public interface AppointmentDao extends CrudRepository<Appointment, Long> {

    List<Appointment> findAll();

    @Query(value = "select new com.userfront.resource.dto.AppointmentSummary("
            + "a.id, a.date, a.location, a.description, a.confirmed, "
            + "u.userId, u.username, u.firstName, u.lastName) "
            + "from Appointment a left join a.user u",
            countQuery = "select count(a) from Appointment a")
    Page<AppointmentSummary> findSummaries(Pageable pageable);
}
