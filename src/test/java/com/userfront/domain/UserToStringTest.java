package com.userfront.domain;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

public class UserToStringTest {

    private static final String HASH = "$2a$12$abcdefghijklmnopqrstuuSECRETHASHVALUE0123456789abcdefg";

    private User user() {
        User user = new User();
        user.setUserId(42L);
        user.setUsername("jdoe");
        user.setPassword(HASH);
        user.setFirstName("John");
        user.setLastName("Doe");
        user.setEmail("jdoe@example.com");
        user.setPhone("555-0100");
        return user;
    }

    @Test
    public void toStringOmitsPasswordAndPersonalData() {
        String text = user().toString();

        assertThat(text, containsString("userId=42"));
        assertThat(text, containsString("username='jdoe'"));
        assertThat(text, not(containsString(HASH)));
        assertThat(text, not(containsString("password")));
        assertThat(text, not(containsString("jdoe@example.com")));
        assertThat(text, not(containsString("555-0100")));
    }

    @Test
    public void appointmentToStringReferencesUserById() {
        User user = user();
        Appointment appointment = new Appointment();
        appointment.setUser(user);
        List<Appointment> appointments = new ArrayList<>();
        appointments.add(appointment);
        user.setAppointmentList(appointments);

        String text = appointment.toString();

        assertThat(text, containsString("userId=42"));
        assertThat(text, not(containsString(HASH)));
        assertThat(user.toString(), not(containsString("Appointment")));
    }
}
