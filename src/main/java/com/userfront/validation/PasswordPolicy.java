package com.userfront.validation;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

@Component
public class PasswordPolicy {

    public static final int MIN_LENGTH = 12;

    /* BCrypt silently ignores anything past 72 bytes. */
    public static final int MAX_LENGTH = 72;

    private static final String COMMON_PASSWORDS_RESOURCE = "common-passwords.txt";

    private final Set<String> commonPasswords;

    public PasswordPolicy() {
        this.commonPasswords = loadCommonPasswords();
    }

    private static Set<String> loadCommonPasswords() {
        Set<String> passwords = new HashSet<>();
        try (InputStream in = new ClassPathResource(COMMON_PASSWORDS_RESOURCE).getInputStream();
             BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String password = line.trim();
                if (!password.isEmpty() && !password.startsWith("#")) {
                    passwords.add(password.toLowerCase());
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Unable to load " + COMMON_PASSWORDS_RESOURCE, e);
        }
        return Collections.unmodifiableSet(passwords);
    }

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

        if (!password.chars().anyMatch(Character::isUpperCase)
                || !password.chars().anyMatch(Character::isLowerCase)
                || !password.chars().anyMatch(Character::isDigit)) {
            errors.add("Password must contain an uppercase letter, a lowercase letter and a digit.");
        }

        if (password.chars().anyMatch(Character::isWhitespace)) {
            errors.add("Password must not contain whitespace.");
        }

        String lowerPassword = password.toLowerCase();

        if (containsIdentifier(lowerPassword, username) || containsIdentifier(lowerPassword, localPart(email))) {
            errors.add("Password must not contain your username or email address.");
        }

        if (commonPasswords.contains(lowerPassword)) {
            errors.add("Password is among the most commonly used passwords, please choose another one.");
        }

        return errors;
    }

    private boolean containsIdentifier(String lowerPassword, String identifier) {
        return identifier != null && identifier.length() >= 3 && lowerPassword.contains(identifier.toLowerCase());
    }

    private String localPart(String email) {
        if (email == null) {
            return null;
        }
        int at = email.indexOf('@');
        return at > 0 ? email.substring(0, at) : email;
    }
}
