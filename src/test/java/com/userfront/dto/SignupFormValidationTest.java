package com.userfront.dto;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;

import org.junit.BeforeClass;
import org.junit.Test;

public class SignupFormValidationTest {

    private static Validator validator;

    @BeforeClass
    public static void setUpValidator() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    private static SignupForm validForm() {
        SignupForm form = new SignupForm();
        form.setFirstName("Ada");
        form.setLastName("Lovelace");
        form.setPhone("555-123-4567");
        form.setEmail("ada@example.com");
        form.setUsername("ada.lovelace");
        form.setPassword("Analytical1");
        return form;
    }

    private static Set<String> invalidFields(SignupForm form) {
        return validator.validate(form).stream()
                .map(ConstraintViolation::getPropertyPath)
                .map(Object::toString)
                .collect(Collectors.toSet());
    }

    @Test
    public void acceptsValidForm() {
        assertTrue(invalidFields(validForm()).isEmpty());
    }

    @Test
    public void rejectsMissingFields() {
        Set<String> fields = invalidFields(new SignupForm());
        assertEquals(6, fields.size());
    }

    @Test
    public void rejectsMalformedEmail() {
        for (String email : new String[] {"not-an-email", "a@b", "a b@example.com", "@example.com"}) {
            SignupForm form = validForm();
            form.setEmail(email);
            assertEquals(email, setOf("email"), invalidFields(form));
        }
    }

    @Test
    public void rejectsBadUsernames() {
        for (String username : new String[] {"ab", "a234567890123456789012345678901234", "bad name", "x<script>", "../admin"}) {
            SignupForm form = validForm();
            form.setUsername(username);
            assertEquals(username, setOf("username"), invalidFields(form));
        }
    }

    @Test
    public void rejectsWeakPasswords() {
        for (String password : new String[] {"a1", "password", "12345678", "has space1", repeat("a1", 33)}) {
            SignupForm form = validForm();
            form.setPassword(password);
            assertEquals(password, setOf("password"), invalidFields(form));
        }
    }

    @Test
    public void rejectsBadPhoneAndOverlongNames() {
        SignupForm form = validForm();
        form.setPhone("call me");
        form.setFirstName(repeat("x", 51));
        assertEquals(setOf("phone", "firstName"), invalidFields(form));
    }

    private static Set<String> setOf(String... values) {
        return new HashSet<>(Arrays.asList(values));
    }

    private static String repeat(String s, int times) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < times; i++) {
            sb.append(s);
        }
        return sb.toString();
    }
}
