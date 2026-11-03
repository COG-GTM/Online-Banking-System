package com.userfront.resource;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.userfront.domain.Appointment;
import com.userfront.domain.PrimaryAccount;
import com.userfront.domain.PrimaryTransaction;
import com.userfront.domain.Recipient;
import com.userfront.domain.SavingsAccount;
import com.userfront.domain.User;
import com.userfront.domain.security.Role;
import com.userfront.domain.security.UserRole;
import com.userfront.service.UserService;

public class UserJsonExposureTest {

    private static final String HASH = "$2a$12$abcdefghijklmnopqrstuvCompromisedHashValue";

    private final ObjectMapper mapper = new ObjectMapper();
    private User user;

    @Before
    public void setUp() {
        PrimaryAccount primary = new PrimaryAccount();
        primary.setId(7L);
        primary.setAccountNumber(11223101);
        primary.setAccountBalance(new BigDecimal("1500.00"));
        primary.setPrimaryTransactionList(Collections.singletonList(new PrimaryTransaction()));

        SavingsAccount savings = new SavingsAccount();
        savings.setId(8L);
        savings.setAccountNumber(11223102);
        savings.setAccountBalance(new BigDecimal("250.00"));

        Recipient recipient = new Recipient();
        recipient.setName("Payee");
        recipient.setAccountNumber("999-PAYEE-ACCT");

        Role role = new Role();
        role.setRoleId(1);
        role.setName("ROLE_USER");

        user = new User();
        user.setUserId(42L);
        user.setUsername("alice");
        user.setPassword(HASH);
        user.setFirstName("Alice");
        user.setLastName("Smith");
        user.setEmail("alice@example.com");
        user.setPhone("555-0100");
        user.setPrimaryAccount(primary);
        user.setSavingsAccount(savings);
        user.setRecipientList(Collections.singletonList(recipient));
        Set<UserRole> roles = new HashSet<>();
        roles.add(new UserRole(user, role));
        user.setUserRoles(roles);
    }

    @Test
    public void adminUserListExposesOnlyProfileAndBalances() throws Exception {
        UserService userService = mock(UserService.class);
        when(userService.findUserList()).thenReturn(Collections.singletonList(user));
        UserResource resource = new UserResource();
        ReflectionTestUtils.setField(resource, "userService", userService);

        String json = mapper.writeValueAsString(resource.userList());
        JsonNode node = mapper.readTree(json).get(0);

        assertFalse(json, json.contains(HASH));
        assertFalse(json, json.contains("999-PAYEE-ACCT"));
        Set<String> fields = new HashSet<>();
        node.fieldNames().forEachRemaining(fields::add);
        assertEquals(new HashSet<>(java.util.Arrays.asList("userId", "username", "firstName", "lastName",
                "email", "phone", "enabled", "primaryAccount", "savingsAccount")), fields);

        Set<String> accountFields = new HashSet<>();
        node.get("primaryAccount").fieldNames().forEachRemaining(accountFields::add);
        assertEquals(new HashSet<>(java.util.Arrays.asList("accountNumber", "accountBalance")), accountFields);
        assertEquals("alice", node.get("username").asText());
        assertEquals(1500.00, node.get("primaryAccount").get("accountBalance").asDouble(), 0.0);
        assertEquals(250.00, node.get("savingsAccount").get("accountBalance").asDouble(), 0.0);
        assertTrue(node.get("enabled").asBoolean());
    }

    @Test
    public void userEntityNeverSerializesCredentialsOrRelations() throws Exception {
        JsonNode node = mapper.readTree(mapper.writeValueAsString(user));

        assertFalse(node.has("password"));
        assertFalse(node.has("recipientList"));
        assertFalse(node.has("userRoles"));
        assertFalse(node.has("appointmentList"));
        assertFalse(node.has("authorities"));
        assertFalse(node.toString().contains(HASH));
    }

    @Test
    public void appointmentUserOmitsCredentialsAndAccounts() throws Exception {
        Appointment appointment = new Appointment();
        appointment.setId(3L);
        appointment.setLocation("Branch");
        appointment.setUser(user);

        JsonNode userNode = mapper.readTree(mapper.writeValueAsString(appointment)).get("user");

        assertEquals("alice", userNode.get("username").asText());
        assertFalse(userNode.has("password"));
        assertFalse(userNode.has("primaryAccount"));
        assertFalse(userNode.has("savingsAccount"));
        assertFalse(userNode.has("recipientList"));
        assertFalse(userNode.has("authorities"));
    }

    @Test
    public void toStringOmitsPasswordAndAssociations() {
        String text = user.toString() + new Appointment().toString();
        Appointment appointment = new Appointment();
        appointment.setUser(user);
        text += appointment.toString();

        assertFalse(text, text.contains(HASH));
        assertFalse(text, text.contains("999-PAYEE-ACCT"));
        assertTrue(text, text.contains("user=alice"));
    }
}
