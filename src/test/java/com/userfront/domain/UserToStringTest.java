package com.userfront.domain;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class UserToStringTest {

    private static final String PASSWORD_HASH = "$2a$12$abcdefghijklmnopqrstuuJ0vC1nYxQ0bN7m8xk3rFqL9cT2eW4yS";

    @Test
    public void toStringDoesNotIncludePassword() {
        User user = new User();
        user.setUsername("jdoe");
        user.setPassword(PASSWORD_HASH);

        String rendered = user.toString();

        assertFalse(rendered.contains(PASSWORD_HASH));
        assertFalse(rendered.contains("password"));
        assertTrue(rendered.contains("username='jdoe'"));
    }
}
