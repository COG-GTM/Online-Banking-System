package com.userfront.service;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.userfront.dao.AppointmentDao;
import com.userfront.domain.Appointment;
import com.userfront.service.UserServiceImpl.AppointmentServiceImpl;

@RunWith(MockitoJUnitRunner.class)
public class AppointmentServiceImplTest {

    @Mock
    private AppointmentDao appointmentDao;

    @InjectMocks
    private AppointmentServiceImpl appointmentService;

    @Test
    public void createAppointmentAlwaysInsertsUnconfirmed() {
        when(appointmentDao.save(any(Appointment.class))).thenAnswer(i -> i.getArgument(0));

        Appointment appointment = new Appointment();
        appointment.setId(42L);
        appointment.setConfirmed(true);

        appointmentService.createAppointment(appointment);

        ArgumentCaptor<Appointment> captor = ArgumentCaptor.forClass(Appointment.class);
        verify(appointmentDao).save(captor.capture());
        assertNull(captor.getValue().getId());
        assertFalse(captor.getValue().isConfirmed());
    }
}
