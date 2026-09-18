package com.userfront.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.springframework.stereotype.Component;

/**
 * Server side password strength rules applied before an account is created.
 */
@Component
public class PasswordPolicy {

    public static final int MIN_LENGTH = 12;
    public static final int MAX_LENGTH = 128;

    private static final Set<String> COMMON_PASSWORDS = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
            "password", "password1", "password123", "passw0rd", "p@ssw0rd", "p@ssword1",
            "123456", "1234567", "12345678", "123456789", "1234567890", "12345678910",
            "qwerty", "qwerty123", "qwertyuiop", "abc123", "abcd1234", "a1b2c3d4",
            "letmein", "welcome", "welcome1", "welcome123", "iloveyou", "admin",
            "administrator", "adminadmin", "monkey", "dragon", "sunshine", "princess",
            "football", "baseball", "superman", "trustno1", "changeme", "secret",
            "banking", "bankpassword", "onlinebanking", "0123456789", "zaq12wsx",
            "1q2w3e4r", "1q2w3e4r5t", "qazwsxedc", "asdfghjkl")));

    /**
     * Returns a human readable message for every rule the password breaks; an empty
     * list means the password is acceptable.
     */
    public List<String> validate(String password, String username, String email) {
        List<String> errors = new ArrayList<>();

        if (password == null || password.isEmpty()) {
            errors.add("Password is required.");
            return errors;
        }

        if (password.length() < MIN_LENGTH) {
            errors.add("Password must be at least " + MIN_LENGTH + " characters long.");
        }

        if (password.length() > MAX_LENGTH) {
            errors.add("Password must be at most " + MAX_LENGTH + " characters long.");
        }

        if (password.trim().isEmpty()) {
            errors.add("Password must not consist only of whitespace.");
        }

        if (!containsCharacterClasses(password)) {
            errors.add("Password must contain an uppercase letter, a lowercase letter, a digit and a special character.");
        }

        String normalized = password.toLowerCase(Locale.ROOT);

        if (isCommon(normalized)) {
            errors.add("Password is too common and has appeared in known breach lists.");
        }

        if (containsIdentifier(normalized, username) || containsIdentifier(normalized, localPart(email))) {
            errors.add("Password must not contain your username or email address.");
        }

        return errors;
    }

    private boolean containsCharacterClasses(String password) {
        boolean upper = false;
        boolean lower = false;
        boolean digit = false;
        boolean special = false;

        for (int i = 0; i < password.length(); i++) {
            char c = password.charAt(i);
            if (Character.isUpperCase(c)) {
                upper = true;
            } else if (Character.isLowerCase(c)) {
                lower = true;
            } else if (Character.isDigit(c)) {
                digit = true;
            } else {
                special = true;
            }
        }

        return upper && lower && digit && special;
    }

    private boolean isCommon(String normalized) {
        if (COMMON_PASSWORDS.contains(normalized)) {
            return true;
        }

        String stripped = normalized.replaceAll("[^a-z0-9]", "");
        return !stripped.isEmpty() && COMMON_PASSWORDS.contains(stripped);
    }

    private boolean containsIdentifier(String normalized, String identifier) {
        if (identifier == null) {
            return false;
        }

        String candidate = identifier.trim().toLowerCase(Locale.ROOT);
        return candidate.length() >= 3 && normalized.contains(candidate);
    }

    private String localPart(String email) {
        if (email == null) {
            return null;
        }

        int at = email.indexOf('@');
        return at > 0 ? email.substring(0, at) : email;
    }
}
