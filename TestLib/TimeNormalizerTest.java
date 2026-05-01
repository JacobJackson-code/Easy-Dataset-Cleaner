import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * JUnit 5 test suite for TimeNormalizer.
 *
 * Default parameters used unless a test states otherwise:
 *   outputFormat      = "HH:mm"
 *   timezoneHandling  = "strip"
 *   globalTimezone    = null
 */
public class TimeNormalizerTest {

    private TimeNormalizer normalizer;

    // Convenience wrapper with all defaults
    private String norm(String input) {
        return normalizer.normalize(input, "HH:mm", "strip", null);
    }

    // Convenience wrapper with custom outputFormat, all other defaults
    private String norm(String input, String outputFormat) {
        return normalizer.normalize(input, outputFormat, "strip", null);
    }

    // Full wrapper
    private String norm(String input, String outputFormat,
                        String timezoneHandling, String globalTimezone) {
        return normalizer.normalize(input, outputFormat, timezoneHandling, globalTimezone);
    }

    @BeforeEach
    void setUp() {
        normalizer = new TimeNormalizer();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GROUP T-1: Null / empty / whitespace
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void t1_1_nullInput() {
        assertNull(norm(null));
    }

    @Test
    void t1_2_emptyString() {
        assertEquals("", norm(""));
    }

    @Test
    void t1_3_whitespaceOnly() {
        // Returns original input as-is (not trimmed)
        assertEquals("   ", norm("   "));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GROUP T-2: Pre-screen failures — not a recognizable time
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void t2_1_plainWord() {
        String result = norm("hello");
        assertTrue(result.startsWith("INVALID_TIME:"),
                "Expected INVALID_TIME, got: " + result);
        assertTrue(result.contains("not a recognizable time"),
                "Expected pre-screen message, got: " + result);
    }

    @Test
    void t2_2_multiWordNonTime() {
        String result = norm("not a time");
        assertTrue(result.startsWith("INVALID_TIME:"),
                "Expected INVALID_TIME, got: " + result);
        assertTrue(result.contains("not a recognizable time"),
                "Expected pre-screen message, got: " + result);
    }

    @Test
    void t2_3_dateStringNoColon() {
        // "2023-01-15" — no colon, no am/pm, not 1-2 digits → fails pre-screen
        String result = norm("2023-01-15");
        assertTrue(result.startsWith("INVALID_TIME:"),
                "Expected INVALID_TIME, got: " + result);
        assertTrue(result.contains("not a recognizable time"),
                "Expected pre-screen message, got: " + result);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GROUP T-3: 24-hour inputs
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void t3_1_24h_standard() {
        assertEquals("14:30", norm("14:30"));
    }

    @Test
    void t3_2_24h_singleDigitHour_leadingZero() {
        assertEquals("09:05", norm("9:05"));
    }

    @Test
    void t3_3_24h_midnight() {
        assertEquals("00:00", norm("00:00"));
    }

    @Test
    void t3_4_24h_maxTime() {
        assertEquals("23:59", norm("23:59"));
    }

    @Test
    void t3_5_24h_withSeconds() {
        assertEquals("14:30:45", norm("14:30:45", "HH:mm:ss"));
    }

    @Test
    void t3_6_24h_noSecondsInput_secondsInOutputFormat() {
        // "14:30" parsed → LocalTime(14,30,0) → formatted with "HH:mm:ss"
        assertEquals("14:30:00", norm("14:30", "HH:mm:ss"));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GROUP T-4: 12-hour inputs
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void t4_1_12h_PM_uppercase() {
        assertEquals("02:30 PM", norm("2:30 PM", "hh:mm a"));
    }

    @Test
    void t4_2_12h_pm_lowercase_caseInsensitive() {
        assertEquals("02:30 PM", norm("2:30 pm", "hh:mm a"));
    }

    @Test
    void t4_3_12h_midnight_AM() {
        // 12:00 AM = 00:00 in 24-hour
        assertEquals("00:00", norm("12:00 AM"));
    }

    @Test
    void t4_4_12h_noon_PM() {
        // 12:00 PM = 12:00 in 24-hour
        assertEquals("12:00", norm("12:00 PM"));
    }

    @Test
    void t4_5_12h_lastMinuteOfDay() {
        assertEquals("23:59", norm("11:59 PM"));
    }

    @Test
    void t4_6_12h_oneAM() {
        assertEquals("01:00", norm("1:00 AM"));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GROUP T-5: AM/PM pre-processing variants
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void t5_1_pmNoSpace() {
        // "2:30pm" → pre-cleaned to "2:30 pm" → 14:30
        assertEquals("14:30", norm("2:30pm"));
    }

    @Test
    void t5_2_pmDotted() {
        // "2:30p.m." → pre-cleaned to "2:30 pm" → 14:30
        assertEquals("14:30", norm("2:30p.m."));
    }

    @Test
    void t5_3_amDotted() {
        // "2:30a.m." → pre-cleaned to "2:30 am" → 02:30
        assertEquals("02:30", norm("2:30a.m."));
    }

    @Test
    void t5_4_leadingAndTrailingWhitespace() {
        assertEquals("14:30", norm(" 14:30 "));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GROUP T-6: Hour-only inputs
    // NOTE: These depend on Java's LocalTime::from defaulting missing
    //       minute to 0. Verify and adjust if the normalizer returns
    //       INVALID_TIME for these instead.
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void t6_1_bareHour_24h() {
        // "14" — single bare hour value
        // VERIFY: expected "14:00" assumes minute defaults to 0
        assertEquals("14:00", norm("14"));
    }

    @Test
    void t6_2_hourWithPM_space() {
        // "2 PM" — hour-only 12h with space
        // VERIFY: expected "02:00 PM" assumes minute defaults to 0
        assertEquals("02:00 PM", norm("2 PM", "hh:mm a"));
    }

    @Test
    void t6_3_hourWithPM_noSpace() {
        // "2PM" → pre-cleaned to "2 PM"
        // VERIFY: expected "14:00" assumes minute defaults to 0
        assertEquals("14:00", norm("2PM"));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GROUP T-7: Timezone abbreviation stripping
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void t7_1_stripTimezoneAbbrev_EST() {
        // "EST" stripped before parse → treated as local time
        assertEquals("14:30", norm("14:30 EST"));
    }

    @Test
    void t7_2_stripTimezoneAbbrev_GMT() {
        assertEquals("14:30", norm("14:30 GMT"));
    }

    @Test
    void t7_3_strip12hWithTimezoneAbbrev() {
        assertEquals("09:00", norm("9:00 AM EST"));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GROUP T-8: timezoneHandling = "strip" with real offset in input
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void t8_1_stripOffset_noSeconds() {
        // Offset present in input but stripped from output
        assertEquals("14:30", norm("14:30 +05:00", "HH:mm", "strip", null));
    }

    @Test
    void t8_2_stripOffset_withSeconds() {
        assertEquals("14:30:00", norm("14:30:00 +05:00", "HH:mm:ss", "strip", null));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GROUP T-9: timezoneHandling = "preserve"
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void t9_1_preserveOffset_offsetPresent() {
        // Offset should be appended to output
        String result = norm("14:30 +05:00", "HH:mm", "preserve", null);
        assertEquals("14:30 +05:00", result);
    }

    @Test
    void t9_2_preserveOffset_noOffsetInInput() {
        // Nothing to preserve — output is plain local time
        assertEquals("14:30", norm("14:30", "HH:mm", "preserve", null));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GROUP T-10: timezoneHandling = convert to UTC
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void t10_1_convertToUTC_positiveFiveHour() {
        // 14:30:00 +05:00 → subtract 5h → 09:30:00 UTC
        assertEquals("09:30:00", norm("14:30:00 +05:00", "HH:mm:ss", "UTC", null));
    }

    @Test
    void t10_2_convertToUTC_alreadyUTC() {
        assertEquals("00:00:00", norm("00:00:00 +00:00", "HH:mm:ss", "UTC", null));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GROUP T-11: globalTimezone — assume source timezone, convert to target
    // NOTE: Result depends on today's date (DST). This test assumes
    //       America/New_York is currently on EDT (UTC-4), valid for
    //       May 1, 2026. Adjust to EST (UTC-5 → "19:30") if test
    //       runs outside EDT dates (roughly Mar–Nov).
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void t11_1_localTimeWithGlobalTimezone_convertToUTC() {
        // 14:30 local (New York EDT = UTC-4) → 18:30 UTC
        // VERIFY: adjust "18:30" → "19:30" if running during EST (UTC-5)
        String result = norm("14:30", "HH:mm", "UTC", "America/New_York");
        assertEquals("18:30", result);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GROUP T-12: Invalid values (pass pre-screen, fail parse)
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void t12_1_invalidHourAndMinute() {
        String result = norm("99:99");
        assertTrue(result.startsWith("INVALID_TIME:"),
                "Expected INVALID_TIME, got: " + result);
    }

    @Test
    void t12_2_hourOutOfRange() {
        String result = norm("25:00");
        assertTrue(result.startsWith("INVALID_TIME:"),
                "Expected INVALID_TIME, got: " + result);
    }
}