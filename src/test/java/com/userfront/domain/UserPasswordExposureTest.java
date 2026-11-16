package com.userfront.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Date;

import org.junit.Before;
import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class UserPasswordExposureTest {

    private static final String HASH = "$2a$12$secretBcryptHashValue";

    private final ObjectMapper mapper = new ObjectMapper();
    private User user;

    @Before
    public void setUp() {
        PrimaryAccount primary = new PrimaryAccount();
        primary.setAccountBalance(new BigDecimal("100.00"));
        SavingsAccount savings = new SavingsAccount();
        savings.setAccountBalance(new BigDecimal("50.00"));

        Recipient recipient = new Recipient();
        recipient.setName("payee");
        recipient.setAccountNumber("12345678");

        user = new User();
        user.setUserId(1L);
        user.setUsername("alice");
        user.setPassword(HASH);
        user.setFirstName("Alice");
        user.setLastName("Smith");
        user.setEmail("alice@example.com");
        user.setPhone("555-0100");
        user.setPrimaryAccount(primary);
        user.setSavingsAccount(savings);
        user.setRecipientList(Collections.singletonList(recipient));
    }

    @Test
    public void userJsonOmitsPasswordAndRecipientsButKeepsAdminFields() throws Exception {
        String json = mapper.writeValueAsString(user);
        JsonNode node = mapper.readTree(json);

        assertFalse(node.has("password"));
        assertFalse(json.contains(HASH));
        assertFalse(node.has("recipientList"));

        assertEquals("alice", node.get("username").asText());
        assertEquals("Alice", node.get("firstName").asText());
        assertEquals("Smith", node.get("lastName").asText());
        assertEquals("alice@example.com", node.get("email").asText());
        assertEquals("555-0100", node.get("phone").asText());
        assertEquals(100.00, node.get("primaryAccount").get("accountBalance").asDouble(), 0.001);
        assertEquals(50.00, node.get("savingsAccount").get("accountBalance").asDouble(), 0.001);
        assertTrue(node.get("enabled").asBoolean());
    }

    @Test
    public void appointmentJsonOmitsUserPasswordAndPiiButKeepsUsername() throws Exception {
        Appointment appointment = new Appointment();
        appointment.setId(7L);
        appointment.setDate(new Date());
        appointment.setDescription("loan");
        appointment.setUser(user);

        String json = mapper.writeValueAsString(appointment);
        JsonNode userNode = mapper.readTree(json).get("user");

        assertFalse(json.contains(HASH));
        assertFalse(userNode.has("password"));
        assertFalse(userNode.has("recipientList"));
        assertFalse(userNode.has("email"));
        assertFalse(userNode.has("phone"));
        assertFalse(userNode.has("primaryAccount"));
        assertFalse(userNode.has("savingsAccount"));
        assertEquals("alice", userNode.get("username").asText());
    }

    @Test
    public void passwordIsStillAcceptedOnDeserialization() throws Exception {
        User parsed = mapper.readValue("{\"username\":\"bob\",\"password\":\"pw\"}", User.class);
        assertEquals("pw", parsed.getPassword());
    }

    @Test
    public void toStringOmitsPassword() {
        assertFalse(user.toString().contains(HASH));
        Appointment appointment = new Appointment();
        appointment.setUser(user);
        assertFalse(appointment.toString().contains(HASH));
    }
}
