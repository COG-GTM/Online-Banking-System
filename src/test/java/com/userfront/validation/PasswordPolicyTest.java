package com.userfront.validation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.Test;

public class PasswordPolicyTest {

    private static final String USERNAME = "jdoe";
    private static final String EMAIL = "john.doe@example.com";

    @Test
    public void acceptsStrongPassword() {
        assertNull(PasswordPolicy.validate("Correct-Horse-42", USERNAME, EMAIL));
    }

    @Test
    public void rejectsMissingPassword() {
        assertEquals("Password is required.", PasswordPolicy.validate(null, USERNAME, EMAIL));
        assertEquals("Password is required.", PasswordPolicy.validate("", USERNAME, EMAIL));
    }

    @Test
    public void rejectsSingleCharacterPassword() {
        assertEquals("Password must be at least 12 characters long.", PasswordPolicy.validate("a", USERNAME, EMAIL));
    }

    @Test
    public void rejectsElevenCharacterPassword() {
        assertNotNull(PasswordPolicy.validate("Abcdefgh-1x", USERNAME, EMAIL));
    }

    @Test
    public void rejectsOverlongPassword() {
        StringBuilder sb = new StringBuilder("Aa1-");
        while (sb.length() <= PasswordPolicy.MAX_LENGTH) {
            sb.append('x');
        }
        assertEquals("Password must be at most 128 characters long.", PasswordPolicy.validate(sb.toString(), USERNAME, EMAIL));
    }

    @Test
    public void rejectsLowComplexityPassword() {
        assertNotNull(PasswordPolicy.validate("alllowercaseletters", USERNAME, EMAIL));
        assertNotNull(PasswordPolicy.validate("lowercase12345", USERNAME, EMAIL));
    }

    @Test
    public void rejectsCommonPassword() {
        assertEquals("Password is too common, please choose a different one.", PasswordPolicy.validate("Password123!", USERNAME, EMAIL));
    }

    @Test
    public void rejectsPasswordContainingUsernameOrEmail() {
        assertNotNull(PasswordPolicy.validate("Secret-JDOE-2024", USERNAME, EMAIL));
        assertNotNull(PasswordPolicy.validate("John.Doe-2024!", "someone", EMAIL));
    }
}
