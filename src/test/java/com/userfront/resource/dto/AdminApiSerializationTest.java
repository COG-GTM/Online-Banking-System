package com.userfront.resource.dto;

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
import com.userfront.domain.Appointment;
import com.userfront.domain.PrimaryAccount;
import com.userfront.domain.Recipient;
import com.userfront.domain.SavingsAccount;
import com.userfront.domain.User;

public class AdminApiSerializationTest {

    private static final String HASH = "$2a$12$abcdefghijklmnopqrstuvCJKLMNOPQRSTUVWXYZ0123456789ab";

    private final ObjectMapper mapper = new ObjectMapper();
    private User user;

    @Before
    public void setUp() {
        PrimaryAccount primary = new PrimaryAccount();
        primary.setId(1L);
        primary.setAccountNumber(11223101);
        primary.setAccountBalance(new BigDecimal("100.00"));

        SavingsAccount savings = new SavingsAccount();
        savings.setId(2L);
        savings.setAccountNumber(11223102);
        savings.setAccountBalance(new BigDecimal("250.00"));

        Recipient recipient = new Recipient();
        recipient.setName("landlord");

        user = new User();
        user.setUserId(7L);
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
    public void userSummaryExposesOnlyAdminFields() throws Exception {
        String json = mapper.writeValueAsString(Collections.singletonList(UserSummary.from(user)));
        JsonNode node = mapper.readTree(json).get(0);

        assertFalse(json.contains(HASH));
        assertFalse(node.has("password"));
        assertFalse(node.has("authorities"));
        assertFalse(node.has("recipientList"));
        assertEquals("alice", node.get("username").asText());
        assertTrue(node.get("enabled").asBoolean());
        assertEquals(0, new BigDecimal("100.00").compareTo(node.get("primaryAccount").get("accountBalance").decimalValue()));
        assertEquals(11223102, node.get("savingsAccount").get("accountNumber").asInt());
    }

    @Test
    public void appointmentSummaryDoesNotEmbedUserEntity() throws Exception {
        Appointment appointment = new Appointment();
        appointment.setId(3L);
        appointment.setDate(new Date(0L));
        appointment.setLocation("Branch 1");
        appointment.setDescription("Loan");
        appointment.setUser(user);

        String json = mapper.writeValueAsString(Collections.singletonList(AppointmentSummary.from(appointment)));
        JsonNode node = mapper.readTree(json).get(0);

        assertFalse(json.contains(HASH));
        assertFalse(node.has("user"));
        assertEquals("alice", node.get("username").asText());
        assertEquals(7L, node.get("userId").asLong());
    }

    @Test
    public void userEntityNeverSerializesPassword() throws Exception {
        user.setRecipientList(null);
        String json = mapper.writeValueAsString(user);

        assertFalse(json.contains(HASH));
        assertFalse(mapper.readTree(json).has("password"));
        assertFalse(user.toString().contains(HASH));
    }
}
