package com.userfront.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

import org.junit.Test;

import com.userfront.controller.AppointmentDateParser.InvalidAppointmentDateException;

public class AppointmentDateParserTest {

    private static final ZoneId ZONE = ZoneId.of("UTC");

    private final AppointmentDateParser parser = new AppointmentDateParser(
            Clock.fixed(LocalDateTime.of(2026, 6, 15, 9, 0).atZone(ZONE).toInstant(), ZONE));

    @Test
    public void parsesValidFutureDate() throws Exception {
        assertEquals(at(2026, 6, 20, 14, 30), parser.parse("2026-06-20 14:30"));
    }

    @Test
    public void noonIsNotShiftedToMidnight() throws Exception {
        assertEquals(at(2026, 6, 20, 12, 30), parser.parse("2026-06-20 12:30"));
    }

    @Test
    public void trimsSurroundingWhitespace() throws Exception {
        assertEquals(at(2026, 6, 20, 8, 0), parser.parse("  2026-06-20 08:00 "));
    }

    @Test
    public void rejectsOutOfRangeMonthDayAndTime() {
        assertRejected("2026-13-01 10:00");
        assertRejected("2026-06-31 10:00");
        assertRejected("2027-02-29 10:00");
        assertRejected("2026-06-20 24:00");
        assertRejected("2026-06-20 10:60");
    }

    @Test
    public void rejectsMalformedInput() {
        assertRejected("2026-06-20 10:00xyz");
        assertRejected("2026-6-20 10:00");
        assertRejected("2026/06/20 10:00");
        assertRejected("2026-06-20");
        assertRejected("tomorrow");
    }

    @Test
    public void rejectsBlankInput() {
        assertRejected(null);
        assertRejected("");
        assertRejected("   ");
    }

    @Test
    public void rejectsPastAndCurrentTimes() {
        assertRejected("2026-06-15 08:59");
        assertRejected("2026-06-15 09:00");
        assertRejected("2013-02-14 10:00");
    }

    @Test
    public void acceptsNextMinute() throws Exception {
        assertEquals(at(2026, 6, 15, 9, 1), parser.parse("2026-06-15 09:01"));
    }

    @Test
    public void repeatedFallBackHourUsesTheFutureOccurrence() throws Exception {
        ZoneId newYork = ZoneId.of("America/New_York");
        Instant now = LocalDateTime.of(2026, 11, 1, 1, 40).atZone(newYork).withLaterOffsetAtOverlap().toInstant();
        AppointmentDateParser fallBack = new AppointmentDateParser(Clock.fixed(now, newYork));

        Date parsed = fallBack.parse("2026-11-01 01:50");

        assertEquals(Date.from(now.plusSeconds(600)), parsed);
        assertRejected(fallBack, "2026-11-01 01:30");
    }

    @Test
    public void firstOccurrenceOfRepeatedHourIsKeptWhenStillAhead() throws Exception {
        ZoneId newYork = ZoneId.of("America/New_York");
        Instant now = LocalDateTime.of(2026, 11, 1, 1, 10).atZone(newYork).withEarlierOffsetAtOverlap().toInstant();
        AppointmentDateParser fallBack = new AppointmentDateParser(Clock.fixed(now, newYork));

        assertEquals(Date.from(now.plusSeconds(1200)), fallBack.parse("2026-11-01 01:30"));
    }

    @Test
    public void nonexistentSpringForwardTimeIsRejected() {
        ZoneId newYork = ZoneId.of("America/New_York");
        AppointmentDateParser springForward = new AppointmentDateParser(
                Clock.fixed(LocalDateTime.of(2026, 3, 7, 9, 0).atZone(newYork).toInstant(), newYork));

        assertRejected(springForward, "2026-03-08 02:30");
    }

    private void assertRejected(String value) {
        assertRejected(parser, value);
    }

    private static void assertRejected(AppointmentDateParser parser, String value) {
        try {
            parser.parse(value);
            fail("Expected rejection of " + value);
        } catch (InvalidAppointmentDateException expected) {
        }
    }

    private static Date at(int year, int month, int day, int hour, int minute) {
        return Date.from(LocalDateTime.of(year, month, day, hour, minute).atZone(ZONE).toInstant());
    }
}
