package com.userfront.service.UserServiceImpl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.userfront.dao.AppointmentDao;
import com.userfront.domain.Appointment;

@RunWith(MockitoJUnitRunner.class)
public class AppointmentServiceImplTest {

    @Mock
    private AppointmentDao appointmentDao;

    @InjectMocks
    private AppointmentServiceImpl appointmentService;

    @Test
    public void findAppointmentReturnsStoredAppointment() {
        Appointment appointment = new Appointment();
        when(appointmentDao.findById(1L)).thenReturn(Optional.of(appointment));

        assertSame(appointment, appointmentService.findAppointment(1L));
    }

    @Test
    public void confirmAppointmentMarksAppointmentConfirmed() {
        Appointment appointment = new Appointment();
        when(appointmentDao.findById(1L)).thenReturn(Optional.of(appointment));

        appointmentService.confirmAppointment(1L);

        assertTrue(appointment.isConfirmed());
        verify(appointmentDao).save(appointment);
    }

    @Test
    public void confirmAppointmentReturnsNotFoundForMissingId() {
        when(appointmentDao.findById(99L)).thenReturn(Optional.empty());

        try {
            appointmentService.confirmAppointment(99L);
            fail("Expected ResponseStatusException");
        } catch (ResponseStatusException e) {
            assertEquals(HttpStatus.NOT_FOUND, e.getStatus());
        }
        verify(appointmentDao, never()).save(any(Appointment.class));
    }
}
