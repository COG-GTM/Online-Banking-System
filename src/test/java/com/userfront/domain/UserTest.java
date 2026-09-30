package com.userfront.domain;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

public class UserTest {

    @Test
    public void toStringOmitsPasswordAndAssociations() {
        User user = new User();
        user.setUserId(42L);
        user.setUsername("jdoe");
        user.setPassword("$2a$12$secretBcryptHashValue");
        user.setEmail("jdoe@example.com");

        Appointment appointment = new Appointment();
        appointment.setUser(user);
        List<Appointment> appointments = new ArrayList<>();
        appointments.add(appointment);
        user.setAppointmentList(appointments);

        String rendered = user.toString();

        assertFalse(rendered.contains("secretBcryptHashValue"));
        assertFalse(rendered.contains("password"));
        assertFalse(rendered.contains("appointmentList"));
        assertTrue(rendered.contains("username='jdoe'"));
        assertFalse(appointment.toString().contains("secretBcryptHashValue"));
    }
}
