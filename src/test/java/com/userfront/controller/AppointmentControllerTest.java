package com.userfront.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.security.Principal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;

import org.junit.Before;
import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.ui.ExtendedModelMap;

import com.userfront.domain.Appointment;
import com.userfront.domain.User;
import com.userfront.service.AppointmentService;
import com.userfront.service.UserService;

public class AppointmentControllerTest {

    private AppointmentController controller;
    private AppointmentService appointmentService;
    private UserService userService;
    private ExtendedModelMap model;
    private Principal principal;
    private User user;

    @Before
    public void setUp() {
        controller = new AppointmentController();
        appointmentService = mock(AppointmentService.class);
        userService = mock(UserService.class);
        ReflectionTestUtils.setField(controller, "appointmentService", appointmentService);
        ReflectionTestUtils.setField(controller, "userService", userService);
        model = new ExtendedModelMap();
        principal = () -> "customer";
        user = new User();
        when(userService.findByUsername("customer")).thenReturn(user);
    }

    @Test
    public void validFutureDateIsSavedWith24HourClock() {
        LocalDateTime future = LocalDateTime.now().plusDays(3).withHour(15).withMinute(30).withSecond(0).withNano(0);
        Appointment appointment = new Appointment();

        String view = controller.createAppointmentPost(appointment,
                future.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")), model, principal);

        assertEquals("redirect:/userFront", view);
        assertEquals(Date.from(future.atZone(ZoneId.systemDefault()).toInstant()), appointment.getDate());
        assertSame(user, appointment.getUser());
        verify(appointmentService).createAppointment(appointment);
    }

    @Test
    public void outOfRangeMonthIsRejected() {
        assertRejected((LocalDateTime.now().getYear() + 1) + "-13-01 10:00");
    }

    @Test
    public void nonexistentDayIsRejected() {
        assertRejected((LocalDateTime.now().getYear() + 1) + "-02-30 10:00");
    }

    @Test
    public void outOfRangeHourIsRejected() {
        assertRejected((LocalDateTime.now().getYear() + 1) + "-06-01 25:00");
    }

    @Test
    public void trailingTextIsRejected() {
        assertRejected((LocalDateTime.now().getYear() + 1) + "-06-01 10:00xyz");
    }

    @Test
    public void garbageIsRejected() {
        assertRejected("not a date");
    }

    @Test
    public void pastDateIsRejected() {
        assertRejected(LocalDateTime.now().minusDays(1).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
    }

    private void assertRejected(String date) {
        String view = controller.createAppointmentPost(new Appointment(), date, model, principal);

        assertEquals("appointment", view);
        assertNotNull(model.get("dateError"));
        verify(appointmentService, never()).createAppointment(any(Appointment.class));
    }
}
