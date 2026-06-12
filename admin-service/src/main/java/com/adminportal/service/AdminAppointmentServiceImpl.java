package com.adminportal.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.userfront.dao.AppointmentDao;
import com.userfront.domain.Appointment;
import com.userfront.service.AppointmentService;

@Service
public class AdminAppointmentServiceImpl implements AppointmentService {

    @Autowired
    private AppointmentDao appointmentDao;

    @Override
    public List<Appointment> findAll() {
        return appointmentDao.findAll();
    }

    @Override
    public Appointment findAppointment(Long id) {
        return appointmentDao.findById(id).orElse(null);
    }

    @Override
    public void confirmAppointment(Long id) {
        Appointment appointment = findAppointment(id);
        appointment.setConfirmed(true);
        appointmentDao.save(appointment);
    }

    @Override
    public Appointment createAppointment(Appointment appointment) {
        throw new UnsupportedOperationException("Not supported by admin-service");
    }
}
