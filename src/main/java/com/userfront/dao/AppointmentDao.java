package com.userfront.dao;

import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import com.userfront.domain.Appointment;
import com.userfront.resource.dto.AppointmentSummary;

public interface AppointmentDao extends CrudRepository<Appointment, Long> {

    List<Appointment> findAll();

    @Query("select new com.userfront.resource.dto.AppointmentSummary(a.id, a.date, a.location, a.description, a.confirmed, u.username) "
            + "from Appointment a left join a.user u order by a.date desc")
    List<AppointmentSummary> findAllSummaries();
}
