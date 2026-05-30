package com.undoschool.bookingsystem.util;

import org.springframework.stereotype.Component;

import java.time.*;
import java.time.format.DateTimeFormatter;

/**
 * All times are stored in UTC (Instant). This utility handles:
 * - Teacher input (LocalDateTime + their ZoneId) → UTC Instant for persistence
 * - UTC Instant → formatted local time string for API responses
 */
@Component
public class TimezoneConverter {

    private static final DateTimeFormatter DISPLAY_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z");

    /**
     * Convert a teacher's local time to UTC for storage.
     *
     * @param localDateTime  The date-time as entered by the teacher (no timezone info)
     * @param ianaTimezone   IANA timezone string, e.g. "Asia/Kolkata"
     * @return               UTC Instant
     */
    public Instant toUtc(LocalDateTime localDateTime, String ianaTimezone) {
        ZoneId zoneId = ZoneId.of(ianaTimezone);
        return ZonedDateTime.of(localDateTime, zoneId).toInstant();
    }

    /**
     * Format a UTC Instant as a human-readable local string for a given timezone.
     *
     * @param utcInstant     UTC timestamp
     * @param ianaTimezone   Viewer's IANA timezone, e.g. "America/New_York"
     * @return               E.g. "2025-06-07 13:30:00 EDT"
     */
    public String toLocalString(Instant utcInstant, String ianaTimezone) {
        ZoneId zoneId = ZoneId.of(ianaTimezone);
        ZonedDateTime local = utcInstant.atZone(zoneId);
        return local.format(DISPLAY_FORMAT);
    }

    /**
     * Validate IANA timezone string.
     */
    public boolean isValidTimezone(String timezone) {
        try {
            ZoneId.of(timezone);
            return true;
        } catch (DateTimeException e) {
            return false;
        }
    }
}
