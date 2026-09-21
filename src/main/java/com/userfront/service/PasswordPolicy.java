package com.userfront.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Component;

@Component
public class PasswordPolicy {

    public static final int MIN_LENGTH = 12;
    public static final int MAX_LENGTH = 128;

    private static final Set<String> COMMON_PASSWORDS = new HashSet<>(Arrays.asList(
            "password", "password1", "password123", "passw0rd", "qwerty", "qwerty123",
            "123456", "1234567", "12345678", "123456789", "1234567890", "iloveyou",
            "letmein", "welcome", "welcome1", "admin", "admin123", "abc123",
            "monkey", "dragon", "football", "baseball", "sunshine", "princess",
            "trustno1", "changeme", "qazwsx", "zaq12wsx", "onlinebanking", "banking123"));

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

        if (!hasUpperCase(password) || !hasLowerCase(password) || !hasDigit(password)) {
            errors.add("Password must contain upper case letters, lower case letters and digits.");
        }

        if (COMMON_PASSWORDS.contains(password.toLowerCase())) {
            errors.add("Password is too common and has appeared in known breaches.");
        }

        if (containsIgnoreCase(password, username) || containsIgnoreCase(password, localPart(email))) {
            errors.add("Password must not contain your username or email address.");
        }

        return errors;
    }

    private static boolean hasUpperCase(String value) {
        for (int i = 0; i < value.length(); i++) {
            if (Character.isUpperCase(value.charAt(i))) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasLowerCase(String value) {
        for (int i = 0; i < value.length(); i++) {
            if (Character.isLowerCase(value.charAt(i))) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasDigit(String value) {
        for (int i = 0; i < value.length(); i++) {
            if (Character.isDigit(value.charAt(i))) {
                return true;
            }
        }
        return false;
    }

    private static boolean containsIgnoreCase(String password, String candidate) {
        return candidate != null && candidate.length() >= 3
                && password.toLowerCase().contains(candidate.toLowerCase());
    }

    private static String localPart(String email) {
        if (email == null) {
            return null;
        }
        int at = email.indexOf('@');
        return at > 0 ? email.substring(0, at) : email;
    }
}
