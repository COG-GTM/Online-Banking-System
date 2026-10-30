package com.userfront.service.UserServiceImpl;

import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
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

import com.userfront.dao.AppointmentDao;
import com.userfront.domain.Appointment;
import com.userfront.service.AppointmentNotFoundException;

@RunWith(MockitoJUnitRunner.class)
public class AppointmentServiceImplTest {

    @Mock
    private AppointmentDao appointmentDao;

    @InjectMocks
    private AppointmentServiceImpl appointmentService;

    @Test
    public void findAppointmentReturnsStoredAppointment() {
        Appointment appointment = new Appointment();
        when(appointmentDao.findById(7L)).thenReturn(Optional.of(appointment));

        assertSame(appointment, appointmentService.findAppointment(7L));
    }

    @Test(expected = AppointmentNotFoundException.class)
    public void findAppointmentThrowsWhenMissing() {
        when(appointmentDao.findById(7L)).thenReturn(Optional.empty());

        appointmentService.findAppointment(7L);
    }

    @Test
    public void confirmAppointmentMarksConfirmedAndSaves() {
        Appointment appointment = new Appointment();
        when(appointmentDao.findById(7L)).thenReturn(Optional.of(appointment));

        appointmentService.confirmAppointment(7L);

        assertTrue(appointment.isConfirmed());
        verify(appointmentDao).save(appointment);
    }

    @Test
    public void confirmAppointmentThrowsNotFoundWithoutSaving() {
        when(appointmentDao.findById(7L)).thenReturn(Optional.empty());

        try {
            appointmentService.confirmAppointment(7L);
        } catch (AppointmentNotFoundException expected) {
            verify(appointmentDao, never()).save(any(Appointment.class));
            return;
        }
        throw new AssertionError("expected AppointmentNotFoundException");
    }
}
