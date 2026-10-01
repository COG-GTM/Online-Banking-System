package com.userfront.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.userfront.domain.Appointment;

public interface AppointmentService {
	Appointment createAppointment(Appointment appointment);

    Page<Appointment> findAll(Pageable pageable);

    Appointment findAppointment(Long id);

    void confirmAppointment(Long id);
}
