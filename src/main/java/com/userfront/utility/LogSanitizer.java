package com.userfront.utility;

/**
 * Escapes request-supplied values before they are written to the log so a
 * value cannot start a new log line or inject terminal control sequences.
 */
public final class LogSanitizer {

    private LogSanitizer() {
    }

    public static String sanitize(String value) {
        if (value == null) {
            return null;
        }
        StringBuilder out = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c == '\r') {
                out.append("\\r");
            } else if (c == '\n') {
                out.append("\\n");
            } else if (c == '\t') {
                out.append("\\t");
            } else if (Character.isISOControl(c) || c == '\u2028' || c == '\u2029') {
                out.append(String.format("\\u%04x", (int) c));
            } else {
                out.append(c);
            }
        }
        return out.toString();
    }
}
