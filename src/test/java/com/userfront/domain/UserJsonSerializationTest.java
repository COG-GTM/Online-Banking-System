package com.userfront.domain;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Collections;

import org.junit.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

public class UserJsonSerializationTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    private User buildUser() {
        Recipient recipient = new Recipient();
        recipient.setName("Payee");
        recipient.setAccountNumber("123456789");

        User user = new User();
        user.setUserId(1L);
        user.setUsername("alice");
        user.setPassword("$2a$12$secretBcryptHash");
        user.setEmail("alice@example.com");
        user.setRecipientList(Collections.singletonList(recipient));
        return user;
    }

    @Test
    public void userJsonOmitsPasswordAndRecipients() throws Exception {
        String json = objectMapper.writeValueAsString(buildUser());

        assertTrue(json.contains("\"username\":\"alice\""));
        assertFalse(json.contains("password"));
        assertFalse(json.contains("secretBcryptHash"));
        assertFalse(json.contains("recipientList"));
        assertFalse(json.contains("123456789"));
    }

    @Test
    public void appointmentJsonOmitsOwnerPassword() throws Exception {
        Appointment appointment = new Appointment();
        appointment.setUser(buildUser());

        String json = objectMapper.writeValueAsString(appointment);

        assertTrue(json.contains("\"username\":\"alice\""));
        assertFalse(json.contains("password"));
        assertFalse(json.contains("secretBcryptHash"));
    }
}
