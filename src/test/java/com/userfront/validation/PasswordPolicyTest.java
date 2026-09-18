package com.userfront.validation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;

public class PasswordPolicyTest {

    private final PasswordPolicy passwordPolicy = new PasswordPolicy();

    @Test
    public void acceptsStrongPassword() {
        assertTrue(validate("Str0ngBankingPass").isEmpty());
    }

    @Test
    public void rejectsMissingPassword() {
        assertEquals(1, validate(null).size());
        assertEquals(1, validate("").size());
    }

    @Test
    public void rejectsShortPassword() {
        assertFalse(validate("Ab3").isEmpty());
    }

    @Test
    public void rejectsPasswordLongerThanBcryptLimit() {
        StringBuilder password = new StringBuilder("Aa1");
        while (password.length() <= PasswordPolicy.MAX_LENGTH) {
            password.append('x');
        }
        assertFalse(validate(password.toString()).isEmpty());
    }

    @Test
    public void rejectsPasswordWithoutMixedCharacterClasses() {
        assertFalse(validate("alllowercaseletters").isEmpty());
        assertFalse(validate("NoDigitsInHere").isEmpty());
    }

    @Test
    public void rejectsPasswordWithWhitespace() {
        assertFalse(validate("Str0ng Banking Pass").isEmpty());
    }

    @Test
    public void rejectsPasswordContainingUsernameOrEmailLocalPart() {
        assertFalse(passwordPolicy.validate("Jsmith1234567", "jsmith", "john@example.com").isEmpty());
        assertFalse(passwordPolicy.validate("John1234567890", "jsmith", "john@example.com").isEmpty());
    }

    @Test
    public void rejectsCommonPassword() {
        assertFalse(validate("Password1234").isEmpty());
        assertFalse(validate("password1234").isEmpty());
    }

    private List<String> validate(String password) {
        return passwordPolicy.validate(password, "jsmith", "john@example.com");
    }
}
