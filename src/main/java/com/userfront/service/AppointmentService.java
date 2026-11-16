package com.userfront.service;

import java.util.List;

import com.userfront.domain.Appointment;
import com.userfront.resource.dto.AppointmentSummary;

public interface AppointmentService {
	Appointment createAppointment(Appointment appointment);

    List<Appointment> findAll();

    List<AppointmentSummary> findAllSummaries();

    Appointment findAppointment(Long id);

    void confirmAppointment(Long id);
}
