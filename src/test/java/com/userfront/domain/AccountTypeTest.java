package com.userfront.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.util.Optional;

import org.junit.Test;

public class AccountTypeTest {

    @Test
    public void knownValuesMapToEnum() {
        assertEquals(Optional.of(AccountType.PRIMARY), AccountType.fromValue("Primary"));
        assertEquals(Optional.of(AccountType.SAVINGS), AccountType.fromValue("Savings"));
    }

    @Test
    public void unknownValuesAreRejected() {
        String[] rejected = {null, "", " ", "primary", "SAVINGS", "Checking", "Primary ", "Primary,Savings"};
        for (String value : rejected) {
            assertFalse("expected rejection of [" + value + "]", AccountType.fromValue(value).isPresent());
        }
    }
}
