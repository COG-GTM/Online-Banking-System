package com.userfront.resource;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.util.Collections;

import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.userfront.domain.Appointment;
import com.userfront.domain.PrimaryAccount;
import com.userfront.domain.SavingsAccount;
import com.userfront.domain.User;
import com.userfront.service.UserService;

public class UserResourceSerializationTest {

    private static final String HASH = "$2a$12$abcdefghijklmnopqrstuuCQ5hQ0x2s8d7Ff6t1e3VvHnq2oJpZ1a";

    private final ObjectMapper mapper = new ObjectMapper();

    private User user() {
        PrimaryAccount primary = new PrimaryAccount();
        primary.setId(1L);
        primary.setAccountNumber(11223101);
        primary.setAccountBalance(new BigDecimal("100.00"));
        SavingsAccount savings = new SavingsAccount();
        savings.setId(2L);
        savings.setAccountNumber(11223102);
        savings.setAccountBalance(new BigDecimal("50.00"));

        User user = new User();
        user.setUserId(7L);
        user.setUsername("alice");
        user.setPassword(HASH);
        user.setFirstName("Alice");
        user.setLastName("Smith");
        user.setEmail("alice@example.com");
        user.setPhone("555-0100");
        user.setPrimaryAccount(primary);
        user.setSavingsAccount(savings);
        return user;
    }

    private UserResource userResource() throws Exception {
        UserService userService = (UserService) Proxy.newProxyInstance(
                UserService.class.getClassLoader(),
                new Class<?>[] { UserService.class },
                (proxy, method, args) -> {
                    if (method.getName().equals("findUserList")) {
                        return Collections.singletonList(user());
                    }
                    throw new UnsupportedOperationException(method.getName());
                });
        UserResource resource = new UserResource();
        Field field = UserResource.class.getDeclaredField("userService");
        field.setAccessible(true);
        field.set(resource, userService);
        return resource;
    }

    @Test
    public void userListResponseOmitsPasswordAndKeepsAdminFields() throws Exception {
        String json = mapper.writeValueAsString(userResource().userList());

        assertFalse(json, json.contains(HASH));
        assertFalse(json, json.contains("\"password\""));
        assertFalse(json, json.contains("authorities"));
        assertFalse(json, json.contains("userRoles"));
        assertFalse(json, json.contains("recipientList"));

        JsonNode node = mapper.readTree(json).get(0);
        assertEquals("alice", node.get("username").asText());
        assertEquals("Alice", node.get("firstName").asText());
        assertEquals("Smith", node.get("lastName").asText());
        assertEquals("alice@example.com", node.get("email").asText());
        assertEquals("555-0100", node.get("phone").asText());
        assertTrue(node.get("enabled").asBoolean());
        assertEquals(0, new BigDecimal("100.00").compareTo(node.get("primaryAccount").get("accountBalance").decimalValue()));
        assertEquals(0, new BigDecimal("50.00").compareTo(node.get("savingsAccount").get("accountBalance").decimalValue()));
    }

    @Test
    public void userEntityNeverSerializesPasswordHash() throws Exception {
        String json = mapper.writeValueAsString(user());

        assertFalse(json, json.contains(HASH));
        assertFalse(json, json.contains("\"password\""));
    }

    @Test
    public void appointmentResponseDoesNotLeakEmbeddedUserPassword() throws Exception {
        Appointment appointment = new Appointment();
        appointment.setUser(user());

        String json = mapper.writeValueAsString(appointment);

        assertFalse(json, json.contains(HASH));
    }

    @Test
    public void userToStringOmitsPassword() {
        assertFalse(user().toString().contains(HASH));
    }
}
