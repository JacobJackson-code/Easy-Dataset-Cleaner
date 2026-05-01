import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * JUnit 5 test suite for DateNormalizer.
 *
 * Default parameters used unless a test states otherwise:
 *   outputFormat        = "yyyy-MM-dd"
 *   suspiciousYearBefore = 0
 *   minYear             = 1900
 *   maxYear             = 2100
 *   secondaryOrder      = null
 */
public class DateNormalizerTest {

    private DateNormalizer normalizer;

    // Convenience wrapper with all defaults
    private String norm(String input, String inputOrder) {
        return normalizer.normalize(input, inputOrder, "yyyy-MM-dd", 0, 1900, 2100, null);
    }

    // Convenience wrapper with suspiciousYearBefore
    private String norm(String input, String inputOrder, int suspiciousYearBefore) {
        return normalizer.normalize(input, inputOrder, "yyyy-MM-dd", suspiciousYearBefore, 1900, 2100, null);
    }

    // Full wrapper
    private String norm(String input, String inputOrder, String outputFormat,
                        int suspiciousYearBefore, int minYear, int maxYear, String secondaryOrder) {
        return normalizer.normalize(input, inputOrder, outputFormat,
                suspiciousYearBefore, minYear, maxYear, secondaryOrder);
    }

    @BeforeEach
    void setUp() {
        normalizer = new DateNormalizer();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GROUP D-1: Null / empty / whitespace
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void d1_1_nullInput() {
        assertNull(norm(null, "MDY"));
    }

    @Test
    void d1_2_emptyString() {
        assertEquals("", norm("", "MDY"));
    }

    @Test
    void d1_3_whitespaceOnly() {
        // Returns original input (not trimmed)
        assertEquals("   ", norm("   ", "MDY"));
    }

    @Test
    void d1_4_inputTooLong() {
        String longInput = "x".repeat(61);
        assertTrue(norm(longInput, "MDY").startsWith("INVALID_DATE:"));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GROUP D-2: Pre-screen — no digits
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void d2_1_noDigitsWord() {
        assertEquals("INVALID_DATE: hello (no numeric content)", norm("hello", "MDY"));
    }

    @Test
    void d2_2_monthNameOnly() {
        assertEquals("INVALID_DATE: March (no numeric content)", norm("March", "MDY"));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GROUP D-3: Compact 8-digit dates
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void d3_1_compactYYYYMMDD_unambiguous() {
        assertEquals("2023-10-25", norm("20231025", "MDY"));
    }

    @Test
    void d3_2_compactLeapYear() {
        assertEquals("2024-02-29", norm("20240229", "MDY"));
    }

    @Test
    void d3_3_compactNonLeapFeb29() {
        // 2023 is not a leap year — no valid YYYYMMDD interpretation
        assertEquals("INVALID_DATE: 20230229 (no valid interpretation of compact date)",
                norm("20230229", "MDY"));
    }

    @Test
    void d3_4_compactOnlyDDMMYYYYValid() {
        // "01022023": YYYYMMDD → year=0102 (invalid), only DDMMYYYY works
        // Code only accepts YYYYMMDD, so this falls to INVALID_DATE
        assertEquals("INVALID_DATE: 01022023 (no valid interpretation of compact date)",
                norm("01022023", "MDY"));
    }

    @Test
    void d3_5_compactYearBeforeThreshold() {
        assertEquals("SUSPICIOUS_DATE: 20231025 (year 2023 before threshold 2024)",
                norm("20231025", "MDY", 2024));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GROUP D-4: ISO datetime component stripping
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void d4_1_isoDatetimeWithZ() {
        assertEquals("2023-05-15", norm("2023-05-15T14:30:00Z", "YMD"));
    }

    @Test
    void d4_2_isoDatetimeWithOffset() {
        assertEquals("2023-05-15", norm("2023-05-15T14:30:00+05:30", "YMD"));
    }

    @Test
    void d4_3_dateWithSpaceAndSeconds() {
        assertEquals("2023-05-15", norm("2023-05-15 14:30:00", "YMD"));
    }

    @Test
    void d4_4_dateWithSpaceNoSeconds() {
        assertEquals("2023-05-15", norm("2023-05-15 14:30", "YMD"));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GROUP D-5: Text month parsing
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void d5_1_textMonthMiddle_MDY() {
        assertEquals("2023-03-15", norm("March 15 2023", "MDY"));
    }

    @Test
    void d5_2_textMonthDayBeforeMonth() {
        assertEquals("2023-03-15", norm("15 March 2023", "MDY"));
    }

    @Test
    void d5_3_textMonthYMD() {
        assertEquals("2023-03-15", norm("2023 March 15", "YMD"));
    }

    @Test
    void d5_4_ordinalAndCommaStripped() {
        assertEquals("2023-03-15", norm("March 15th, 2023", "MDY"));
    }

    @Test
    void d5_5_junkWordDiscarded() {
        // "the" is an alpha-only token, discarded by pre-clean
        assertEquals("2023-01-15", norm("January the 15th 2023", "MDY"));
    }

    @Test
    void d5_6_septAbbreviation() {
        assertEquals("2021-09-05", norm("Sept 5 2021", "MDY"));
    }

    @Test
    void d5_7_leapDayValidYear() {
        assertEquals("2024-02-29", norm("Feb 29 2024", "MDY"));
    }

    @Test
    void d5_8_leapDayInvalidYear() {
        // 2023 is not a leap year
        assertEquals("INVALID_DATE: Feb 29 2023 (invalid date: 2023-02-29)",
                norm("Feb 29 2023", "MDY"));
    }

    @Test
    void d5_9_textMonthMissingNumericComponent() {
        // Only one numeric token — expects 2
        assertEquals("INVALID_DATE: March 2023 (expected 2 numeric components with text month)",
                norm("March 2023", "MDY"));
    }

    @Test
    void d5_10_textMonthTwoFourDigitNumbers() {
        assertEquals("SUSPICIOUS_DATE: March 2022 2023 (ambiguous — two 4-digit numbers with text month)",
                norm("March 2022 2023", "MDY"));
    }

    @Test
    void d5_11_historicalDateBeforeThreshold() {
        String result = normalizer.normalize("July 4 1776", "MDY", "yyyy-MM-dd",
                1900, 1000, 2100, null);
        assertEquals("SUSPICIOUS_DATE: July 4 1776 (year 1776 before threshold 1900)", result);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GROUP D-6a: resolveNumeric — Case A, clear unambiguous cases
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void d6a_1_MDY_slashSeparator() {
        assertEquals("2023-01-15", norm("01/15/2023", "MDY"));
    }

    @Test
    void d6a_2_DMY_slashSeparator() {
        assertEquals("2023-01-15", norm("15/01/2023", "DMY"));
    }

    @Test
    void d6a_3_YMD_slashSeparator() {
        assertEquals("2023-01-15", norm("2023/01/15", "YMD"));
    }

    @Test
    void d6a_4_YMD_dotSeparator() {
        assertEquals("2023-01-15", norm("2023.01.15", "YMD"));
    }

    @Test
    void d6a_5_YMD_spaceSeparator() {
        assertEquals("2023-01-15", norm("2023 01 15", "YMD"));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GROUP D-6b: resolveNumeric — swapped month/day detected
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void d6b_6_MDY_dayInMonthSlot() {
        // "15/01/2023" with MDY — 15 is in the month slot, clearly swapped
        String result = norm("15/01/2023", "MDY");
        assertTrue(result.startsWith("SUSPICIOUS_DATE:"),
                "Expected SUSPICIOUS_DATE, got: " + result);
        assertTrue(result.contains("month/day appear swapped"),
                "Expected swap message, got: " + result);
    }

    @Test
    void d6b_7_YMD_dayInMonthSlot() {
        // "2023/15/01" with YMD — 15 is in the month slot
        String result = norm("2023/15/01", "YMD");
        assertTrue(result.startsWith("SUSPICIOUS_DATE:"),
                "Expected SUSPICIOUS_DATE, got: " + result);
        assertTrue(result.contains("month/day appear swapped"),
                "Expected swap message, got: " + result);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GROUP D-6c: resolveNumeric — both arrangements valid (ambiguous)
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void d6c_8_ambiguous_MDY_noSecondary() {
        // 2023/05/06 — month=5,day=6 or month=6,day=5, both valid
        // Year NOT in preferred MDY position → SUSPICIOUS
        String result = norm("2023/05/06", "MDY");
        assertTrue(result.startsWith("SUSPICIOUS_DATE:"),
                "Expected SUSPICIOUS_DATE, got: " + result);
        assertTrue(result.contains("preferred MDY"),
                "Expected preferred-order note, got: " + result);
    }

    @Test
    void d6c_9_ambiguous_YMD_yearInPosition() {
        // Year IS in the preferred YMD position → clean result
        assertEquals("2023-05-06", norm("2023/05/06", "YMD"));
    }

    @Test
    void d6c_10_ambiguous_MDY_withSecondary() {
        // Secondary order YMD breaks the tie → clean result
        String result = norm("2023/05/06", "MDY", "yyyy-MM-dd",
                0, 1900, 2100, "YMD");
        assertEquals("2023-05-06", result);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GROUP D-6d: Year outside allowed range
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void d6d_11_yearBelowMinYear() {
        String result = norm("01/15/1800", "MDY");
        assertTrue(result.startsWith("SUSPICIOUS_DATE:"),
                "Expected SUSPICIOUS_DATE, got: " + result);
        assertTrue(result.contains("outside range"),
                "Expected range message, got: " + result);
    }

    @Test
    void d6d_12_yearAboveMaxYear() {
        String result = norm("01/15/2200", "MDY");
        assertTrue(result.startsWith("SUSPICIOUS_DATE:"),
                "Expected SUSPICIOUS_DATE, got: " + result);
        assertTrue(result.contains("outside range"),
                "Expected range message, got: " + result);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GROUP D-6e: Invalid calendar dates
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void d6e_13_feb29NonLeapYear() {
        String result = norm("02/29/2023", "MDY");
        assertTrue(result.startsWith("INVALID_DATE:"),
                "Expected INVALID_DATE, got: " + result);
        assertTrue(result.contains("no valid month/day combination"),
                "Expected month/day message, got: " + result);
    }

    @Test
    void d6e_14_impossibleMonthAndDay() {
        String result = norm("13/32/2023", "MDY");
        assertTrue(result.startsWith("INVALID_DATE:"),
                "Expected INVALID_DATE, got: " + result);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GROUP D-6f: Bad token lengths (3-digit or 5+ digit tokens)
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void d6f_15_threeDigitToken() {
        String result = norm("201/01/15", "MDY");
        assertTrue(result.startsWith("INVALID_DATE:"),
                "Expected INVALID_DATE, got: " + result);
        assertTrue(result.contains("invalid length"),
                "Expected length message, got: " + result);
    }

    @Test
    void d6f_16_fiveDigitToken() {
        String result = norm("20231/01/15", "MDY");
        assertTrue(result.startsWith("INVALID_DATE:"),
                "Expected INVALID_DATE, got: " + result);
        assertTrue(result.contains("invalid length"),
                "Expected length message, got: " + result);
    }

    @Test
    void d6f_17_multipleFourDigitNumbers() {
        assertEquals("INVALID_DATE: 2023/2024/15 (multiple 4-digit numbers)",
                norm("2023/2024/15", "MDY"));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GROUP D-6g: Suspicious year before threshold
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void d6g_18_yearBeforeThreshold() {
        String result = normalizer.normalize("06/15/1950", "MDY", "yyyy-MM-dd",
                1980, 1900, 2100, null);
        assertTrue(result.startsWith("SUSPICIOUS_DATE:"),
                "Expected SUSPICIOUS_DATE, got: " + result);
        assertTrue(result.contains("before threshold"),
                "Expected threshold message, got: " + result);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GROUP D-6h: Wrong number of tokens
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void d6h_19_twoNumericTokens() {
        String result = norm("2023/01", "MDY");
        assertTrue(result.startsWith("INVALID_DATE:"),
                "Expected INVALID_DATE, got: " + result);
        assertTrue(result.contains("expected 3 date components"),
                "Expected component-count message, got: " + result);
    }

    @Test
    void d6h_20_fourNumericTokens() {
        String result = norm("2023/01/15/16", "MDY");
        assertTrue(result.startsWith("INVALID_DATE:"),
                "Expected INVALID_DATE, got: " + result);
        assertTrue(result.contains("expected 3 date components"),
                "Expected component-count message, got: " + result);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GROUP D-6i: Output format variation
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void d6i_21_outputFormatMMddyyyy() {
        String result = normalizer.normalize("2023/06/15", "YMD", "MM/dd/yyyy",
                0, 1900, 2100, null);
        assertEquals("06/15/2023", result);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GROUP D-7: resolveNumeric — Case B (2-digit year, no 4-digit token)
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void d7_1_twoDigitYear_multipleInterpretations_noSecondary() {
        // "12/05/23" → year=23+2000=2023, both 2023-12-05 and 2023-05-12 valid
        String result = norm("12/05/23", "MDY");
        assertTrue(result.startsWith("SUSPICIOUS_DATE:"),
                "Expected SUSPICIOUS_DATE, got: " + result);
        assertTrue(result.contains("2-digit year"),
                "Expected 2-digit year message, got: " + result);
        assertTrue(result.contains("multiple interpretations"),
                "Expected multiple-interpretations note, got: " + result);
    }

    @Test
    void d7_2_twoDigitYear_multipleInterpretations_secondaryBreaksTie() {
        // Secondary order MDY selects month=12, day=5 → 2023-12-05
        String result = norm("12/05/23", "MDY", "yyyy-MM-dd", 0, 1900, 2100, "MDY");
        assertEquals("2023-12-05", result);
    }

    @Test
    void d7_3_twoDigitYear_forcedByValue_singleInterpretation() {
        // "06/15/85" → 85 is in 32-99 so forced as year (2085)
        // Only one valid arrangement (06=month, 15=day) → INVALID_DATE (malformed)
        String result = norm("06/15/85", "MDY");
        assertTrue(result.startsWith("INVALID_DATE:"),
                "Expected INVALID_DATE, got: " + result);
        assertTrue(result.contains("malformed") && result.contains("2-digit year"),
                "Expected malformed/2-digit year message, got: " + result);
    }

    @Test
    void d7_4_multipleForcedTwoDigitYears() {
        // Both 85 and 90 are in 32-99 range
        String result = norm("85/90/15", "MDY");
        assertTrue(result.startsWith("SUSPICIOUS_DATE:"),
                "Expected SUSPICIOUS_DATE, got: " + result);
        assertTrue(result.contains("multiple values that can only be 2-digit years"),
                "Expected multiple-year-candidates message, got: " + result);
    }
}
