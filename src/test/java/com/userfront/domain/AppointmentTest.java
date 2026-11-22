package com.userfront.domain;

import static org.junit.Assert.assertFalse;

import org.junit.Test;

public class AppointmentTest {

    @Test
    public void newAppointmentIsUnconfirmed() {
        assertFalse(new Appointment().isConfirmed());
    }
}
