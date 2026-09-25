package com.userfront.service.UserServiceImpl;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.userfront.dao.AppointmentDao;
import com.userfront.domain.Appointment;
import com.userfront.service.AppointmentService;

@Service
public class AppointmentServiceImpl implements AppointmentService {

    @Autowired
    private AppointmentDao appointmentDao;

    public Appointment createAppointment(Appointment appointment) {
       return appointmentDao.save(appointment);
    }

    public List<Appointment> findAll() {
        return appointmentDao.findAll();
    }

    public Appointment findAppointment(Long id) {
        return appointmentDao.findById(id).orElse(null);
    }

    public void confirmAppointment(Long id) {
        Appointment appointment = appointmentDao.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Appointment " + id + " not found"));
        appointment.setConfirmed(true);
        appointmentDao.save(appointment);
    }
}
