package com.userfront.domain;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Collections;

import org.junit.Before;
import org.junit.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

public class UserJsonSerializationTest {

    private static final String HASH = "$2a$12$abcdefghijklmnopqrstuuR1pW2bYxq3zv0N8Hn6uQm6xGmF1fO8a";

    private final ObjectMapper mapper = new ObjectMapper();
    private User user;

    @Before
    public void setUp() {
        user = new User();
        user.setUserId(1L);
        user.setUsername("alice");
        user.setPassword(HASH);
        user.setFirstName("Alice");
        user.setEmail("alice@example.com");

        Recipient recipient = new Recipient();
        recipient.setName("Bob");
        recipient.setAccountNumber("123456");
        recipient.setUser(user);
        user.setRecipientList(Collections.singletonList(recipient));
    }

    @Test
    public void userJsonOmitsPasswordAndRecipients() throws Exception {
        String json = mapper.writeValueAsString(user);

        assertTrue(json.contains("\"username\":\"alice\""));
        assertFalse(json.contains("\"password\""));
        assertFalse(json.contains(HASH));
        assertFalse(json.contains("recipientList"));
        assertFalse(json.contains("123456"));
    }

    @Test
    public void appointmentJsonOmitsUserPassword() throws Exception {
        Appointment appointment = new Appointment();
        appointment.setId(7L);
        appointment.setUser(user);

        String json = mapper.writeValueAsString(Collections.singletonList(appointment));

        assertTrue(json.contains("\"username\":\"alice\""));
        assertFalse(json.contains("\"password\""));
        assertFalse(json.contains(HASH));
    }

    @Test
    public void toStringOmitsPassword() {
        Appointment appointment = new Appointment();
        appointment.setUser(user);

        assertFalse(user.toString().contains(HASH));
        assertFalse(appointment.toString().contains(HASH));
        assertTrue(appointment.toString().contains("alice"));
    }
}
