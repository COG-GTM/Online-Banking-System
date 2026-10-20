package com.userfront.service.UserServiceImpl;

import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
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

@RunWith(MockitoJUnitRunner.class)
public class AppointmentServiceImplTest {

    @Mock
    private AppointmentDao appointmentDao;

    @InjectMocks
    private AppointmentServiceImpl appointmentService;

    @Test
    public void findAppointmentLoadsById() {
        Appointment appointment = new Appointment();
        when(appointmentDao.findById(7L)).thenReturn(Optional.of(appointment));

        assertSame(appointment, appointmentService.findAppointment(7L));
    }

    @Test
    public void confirmAppointmentMarksAndSaves() {
        Appointment appointment = new Appointment();
        when(appointmentDao.findById(7L)).thenReturn(Optional.of(appointment));

        appointmentService.confirmAppointment(7L);

        assertTrue(appointment.isConfirmed());
        verify(appointmentDao).save(appointment);
    }
}
