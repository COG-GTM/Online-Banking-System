package com.userfront.domain;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

public class UserToStringTest {

    private static final String PASSWORD_HASH = "$2a$12$abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ012";

    private User user() {
        User user = new User();
        user.setUserId(7L);
        user.setUsername("jdoe");
        user.setPassword(PASSWORD_HASH);
        user.setFirstName("Jane");
        user.setLastName("Doe");
        user.setEmail("jane@example.com");
        user.setPhone("5551234567");
        return user;
    }

    @Test
    public void omitsPasswordAndPersonalData() {
        String text = user().toString();

        assertTrue(text.contains("userId=7"));
        assertTrue(text.contains("username='jdoe'"));
        assertFalse(text.contains("password"));
        assertFalse(text.contains(PASSWORD_HASH));
        assertFalse(text.contains("jane@example.com"));
        assertFalse(text.contains("5551234567"));
    }

    @Test
    public void omitsNestedCollections() {
        User user = user();
        Appointment appointment = new Appointment();
        appointment.setUser(user);
        List<Appointment> appointments = new ArrayList<>();
        appointments.add(appointment);
        user.setAppointmentList(appointments);

        String text = appointment.toString();

        assertTrue(text.contains("username='jdoe'"));
        assertFalse(text.contains(PASSWORD_HASH));
        assertFalse(user.toString().contains("appointmentList"));
    }
}
