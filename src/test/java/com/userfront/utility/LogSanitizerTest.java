package com.userfront.utility;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

public class LogSanitizerTest {

    @Test
    public void escapesCarriageReturnAndLineFeed() {
        assertEquals("bob\\r\\nWARN forged", LogSanitizer.sanitize("bob\r\nWARN forged"));
    }

    @Test
    public void escapesTabAndOtherControlCharacters() {
        assertEquals("a\\tb\\u001b[31m\\u0000\\u0085\\u2028\\u2029",
                LogSanitizer.sanitize("a\tb\u001b[31m\u0000\u0085\u2028\u2029"));
    }

    @Test
    public void leavesOrdinaryUsernamesUnchanged() {
        assertEquals("j.doe-42@example.com", LogSanitizer.sanitize("j.doe-42@example.com"));
        assertEquals("Zoë", LogSanitizer.sanitize("Zoë"));
    }

    @Test
    public void passesNullThrough() {
        assertNull(LogSanitizer.sanitize(null));
    }
}
