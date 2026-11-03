package com.userfront.controller;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.Date;

import org.springframework.stereotype.Component;

@Component
public class AppointmentDateParser {

    public static final String PATTERN = "uuuu-MM-dd HH:mm";

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern(PATTERN).withResolverStyle(ResolverStyle.STRICT);

    private final Clock clock;

    public AppointmentDateParser() {
        this(Clock.systemDefaultZone());
    }

    AppointmentDateParser(Clock clock) {
        this.clock = clock;
    }

    public Date parse(String value) throws InvalidAppointmentDateException {
        if (value == null || value.trim().isEmpty()) {
            throw new InvalidAppointmentDateException("Please pick a date and time for the appointment.");
        }

        LocalDateTime dateTime;
        try {
            dateTime = LocalDateTime.parse(value.trim(), FORMATTER);
        } catch (DateTimeParseException e) {
            throw new InvalidAppointmentDateException("Please enter a valid date and time in the format yyyy-mm-dd hh:mm (24-hour).");
        }

        ZonedDateTime zoned = dateTime.atZone(clock.getZone());
        if (!zoned.toLocalDateTime().equals(dateTime)) {
            throw new InvalidAppointmentDateException("That time does not exist because of a daylight saving change. Please pick another time.");
        }

        Instant now = clock.instant();
        if (!zoned.toInstant().isAfter(now)) {
            zoned = zoned.withLaterOffsetAtOverlap();
        }
        if (!zoned.toInstant().isAfter(now)) {
            throw new InvalidAppointmentDateException("The appointment must be scheduled in the future.");
        }

        return Date.from(zoned.toInstant());
    }

    public static class InvalidAppointmentDateException extends Exception {

        public InvalidAppointmentDateException(String message) {
            super(message);
        }
    }
}
