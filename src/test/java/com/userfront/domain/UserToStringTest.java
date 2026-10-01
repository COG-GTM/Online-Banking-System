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
        return user;
    }

    @Test
    public void omitsPassword() {
        String text = user().toString();

        assertTrue(text.contains("userId=7"));
        assertTrue(text.contains("username='jdoe'"));
        assertFalse(text.contains("password"));
        assertFalse(text.contains(PASSWORD_HASH));
    }

    @Test
    public void omitsNestedCollections() {
        User user = user();
        Appointment appointment = new Appointment();
        appointment.setUser(user);
        List<Appointment> appointments = new ArrayList<>();
        appointments.add(appointment);
        user.setAppointmentList(appointments);
        user.setRecipientList(new ArrayList<Recipient>());

        String text = user.toString();

        assertFalse(text.contains("appointmentList"));
        assertFalse(text.contains("recipientList"));
        assertFalse(text.contains("userRoles"));
        assertFalse(appointment.toString().contains(PASSWORD_HASH));
    }
}
