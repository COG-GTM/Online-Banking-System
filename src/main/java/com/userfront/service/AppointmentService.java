package com.userfront.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.userfront.domain.Appointment;
import com.userfront.resource.dto.AppointmentSummary;

public interface AppointmentService {
	Appointment createAppointment(Appointment appointment);

    List<Appointment> findAll();

    Page<AppointmentSummary> findSummaries(Pageable pageable);

    Appointment findAppointment(Long id);

    void confirmAppointment(Long id);
}
