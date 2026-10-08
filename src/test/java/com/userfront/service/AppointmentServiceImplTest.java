package com.userfront.service;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.userfront.dao.AppointmentDao;
import com.userfront.domain.Appointment;
import com.userfront.exception.AppointmentNotFoundException;
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

    @Test
    public void confirmAppointmentMarksStoredAppointmentConfirmed() {
        Appointment stored = new Appointment();
        stored.setId(7L);
        when(appointmentDao.findById(7L)).thenReturn(Optional.of(stored));

        appointmentService.confirmAppointment(7L);

        assertTrue(stored.isConfirmed());
        verify(appointmentDao).save(stored);
    }

    @Test(expected = AppointmentNotFoundException.class)
    public void confirmAppointmentRejectsUnknownId() {
        when(appointmentDao.findById(99L)).thenReturn(Optional.empty());

        try {
            appointmentService.confirmAppointment(99L);
        } finally {
            verify(appointmentDao, never()).save(any(Appointment.class));
        }
    }
}
