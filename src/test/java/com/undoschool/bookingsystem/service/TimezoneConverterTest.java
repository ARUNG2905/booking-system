package com.undoschool.bookingsystem.service;

import com.undoschool.bookingsystem.util.TimezoneConverter;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;

class TimezoneConverterTest {

    private final TimezoneConverter converter = new TimezoneConverter();

    @Test
    void toUtc_convertIST_toCorrectUtc() {
        // 6 PM IST = 12:30 UTC (IST = UTC+5:30)
        LocalDateTime ist6pm = LocalDateTime.of(2025, 6, 7, 18, 0, 0);
        Instant utc = converter.toUtc(ist6pm, "Asia/Kolkata");

        // 18:00 IST - 5:30 = 12:30 UTC
        assertThat(utc.toString()).isEqualTo("2025-06-07T12:30:00Z");
    }

    @Test
    void toUtc_convertEST_toCorrectUtc() {
        // 6 PM EST (UTC-5) = 23:00 UTC
        LocalDateTime est6pm = LocalDateTime.of(2025, 6, 7, 18, 0, 0);
        Instant utc = converter.toUtc(est6pm, "America/New_York");

        // 18:00 EDT (UTC-4 in June) = 22:00 UTC
        assertThat(utc.toString()).isEqualTo("2025-06-07T22:00:00Z");
    }

    @Test
    void toLocalString_formatsCorrectly() {
        Instant utc = Instant.parse("2025-06-07T12:30:00Z");
        String local = converter.toLocalString(utc, "Asia/Kolkata");
        assertThat(local).contains("18:00:00");
        assertThat(local).contains("IST");
    }

    @Test
    void isValidTimezone_validZone_returnsTrue() {
        assertThat(converter.isValidTimezone("Asia/Kolkata")).isTrue();
        assertThat(converter.isValidTimezone("America/New_York")).isTrue();
        assertThat(converter.isValidTimezone("UTC")).isTrue();
        assertThat(converter.isValidTimezone("Europe/London")).isTrue();
    }

    @Test
    void isValidTimezone_invalidZone_returnsFalse() {
        assertThat(converter.isValidTimezone("Invalid/Zone")).isFalse();
        assertThat(converter.isValidTimezone("IST")).isFalse();
        assertThat(converter.isValidTimezone("")).isFalse();
    }
}
