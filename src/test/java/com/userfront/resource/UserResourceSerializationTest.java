package com.userfront.resource;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Collections;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.userfront.domain.Appointment;
import com.userfront.domain.PrimaryAccount;
import com.userfront.domain.SavingsAccount;
import com.userfront.domain.User;
import com.userfront.service.TransactionService;
import com.userfront.service.UserService;

@RunWith(MockitoJUnitRunner.class)
public class UserResourceSerializationTest {

    private static final String HASH = "$2a$12$abcdefghijklmnopqrstuuK9sQnYb3m5c1u0p6m3b2a1z0y9x8w7v";

    @Mock
    private UserService userService;

    @Mock
    private TransactionService transactionService;

    @InjectMocks
    private UserResource userResource;

    private final ObjectMapper mapper = new ObjectMapper();

    private User user;

    @Before
    public void setUp() {
        PrimaryAccount primary = new PrimaryAccount();
        primary.setAccountNumber(11223101);
        primary.setAccountBalance(new BigDecimal("150.00"));
        SavingsAccount savings = new SavingsAccount();
        savings.setAccountNumber(11223102);
        savings.setAccountBalance(new BigDecimal("2500.00"));

        user = new User();
        user.setUserId(7L);
        user.setUsername("jdoe");
        user.setPassword(HASH);
        user.setFirstName("Jane");
        user.setLastName("Doe");
        user.setEmail("jdoe@example.com");
        user.setPhone("5551234");
        user.setPrimaryAccount(primary);
        user.setSavingsAccount(savings);
    }

    @Test
    public void userListReturnsOnlyAdminPortalFields() throws Exception {
        when(userService.findUserList()).thenReturn(Collections.singletonList(user));

        String json = mapper.writeValueAsString(userResource.userList());
        JsonNode node = mapper.readTree(json).get(0);

        assertFalse(json.contains(HASH));
        assertFalse(node.has("password"));
        assertFalse(node.has("authorities"));
        assertFalse(node.has("userRoles"));
        assertFalse(node.has("recipientList"));
        assertEquals("jdoe", node.get("username").asText());
        assertEquals("Jane", node.get("firstName").asText());
        assertEquals("Doe", node.get("lastName").asText());
        assertEquals("jdoe@example.com", node.get("email").asText());
        assertEquals("5551234", node.get("phone").asText());
        assertTrue(node.get("enabled").asBoolean());
        assertEquals(150.00, node.get("primaryAccount").get("accountBalance").asDouble(), 0.0);
        assertEquals(2500.00, node.get("savingsAccount").get("accountBalance").asDouble(), 0.0);
    }

    @Test
    public void userEntityNeverSerializesPasswordHash() throws Exception {
        Appointment appointment = new Appointment();
        appointment.setUser(user);

        assertFalse(mapper.writeValueAsString(user).contains(HASH));
        assertFalse(mapper.writeValueAsString(appointment).contains(HASH));
        assertFalse(user.toString().contains(HASH));
    }
}
