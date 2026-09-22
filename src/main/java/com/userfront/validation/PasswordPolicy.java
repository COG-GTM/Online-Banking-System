package com.userfront.validation;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public final class PasswordPolicy {

    public static final int MIN_LENGTH = 12;
    public static final int MAX_LENGTH = 128;

    private static final Set<String> COMMON_PASSWORDS = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
            "123456789012", "1234567890123", "password1234", "passw0rd1234", "qwertyuiop12",
            "administrator", "iloveyou1234", "letmein12345", "welcome123456", "abcd1234abcd",
            "onlinebanking", "bankpassword", "trustno1trustno1", "monkey123456", "football1234")));

    private PasswordPolicy() {
    }

    public static String validate(String password, String username, String email) {
        if (password == null || password.isEmpty()) {
            return "Password is required.";
        }

        if (password.length() < MIN_LENGTH) {
            return "Password must be at least " + MIN_LENGTH + " characters long.";
        }

        if (password.length() > MAX_LENGTH) {
            return "Password must be at most " + MAX_LENGTH + " characters long.";
        }

        if (password.trim().length() != password.length()) {
            return "Password must not start or end with whitespace.";
        }

        if (characterClasses(password) < 3) {
            return "Password must contain at least three of: lowercase letters, uppercase letters, digits, symbols.";
        }

        String lowerPassword = password.toLowerCase(Locale.ENGLISH);

        if (COMMON_PASSWORDS.contains(lowerPassword)) {
            return "Password is too common, please choose a different one.";
        }

        if (containsIdentifier(lowerPassword, username) || containsIdentifier(lowerPassword, localPart(email))) {
            return "Password must not contain your username or email address.";
        }

        return null;
    }

    private static int characterClasses(String password) {
        boolean lower = false;
        boolean upper = false;
        boolean digit = false;
        boolean symbol = false;

        for (int i = 0; i < password.length(); i++) {
            char c = password.charAt(i);
            if (Character.isLowerCase(c)) {
                lower = true;
            } else if (Character.isUpperCase(c)) {
                upper = true;
            } else if (Character.isDigit(c)) {
                digit = true;
            } else {
                symbol = true;
            }
        }

        return (lower ? 1 : 0) + (upper ? 1 : 0) + (digit ? 1 : 0) + (symbol ? 1 : 0);
    }

    private static boolean containsIdentifier(String lowerPassword, String identifier) {
        if (identifier == null) {
            return false;
        }

        String candidate = identifier.trim().toLowerCase(Locale.ENGLISH);

        return candidate.length() >= 4 && lowerPassword.contains(candidate);
    }

    private static String localPart(String email) {
        if (email == null) {
            return null;
        }

        int at = email.indexOf('@');

        return at > 0 ? email.substring(0, at) : email;
    }
}
