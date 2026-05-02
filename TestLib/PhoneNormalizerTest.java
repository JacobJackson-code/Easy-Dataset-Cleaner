import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class PhoneNormalizerTest {

    private PhoneNormalizer normalizer;

    @BeforeEach
    void setUp() {
        normalizer = new PhoneNormalizer();
    }

    // ── Group 1: Various input formats → same US national output ─────────────
    // Confirms that punctuation, spaces, and dots are all cleaned identically.

    @Test
    void usInput_parenthesesFormat_toNational() {
        assertEquals("(212) 555-1234", normalizer.normalize("(212) 555-1234", "national", "US"));
    }

    @Test
    void usInput_rawDigits_toNational() {
        assertEquals("(212) 555-1234", normalizer.normalize("2125551234", "national", "US"));
    }

    @Test
    void usInput_dashFormat_toNational() {
        assertEquals("(212) 555-1234", normalizer.normalize("212-555-1234", "national", "US"));
    }

    @Test
    void usInput_dotsFormat_toNational() {
        assertEquals("(212) 555-1234", normalizer.normalize("212.555.1234", "national", "US"));
    }

    // ── Group 2: One valid US number → all output formats ────────────────────

    @Test
    void usNumber_toE164() {
        assertEquals("+12125551234", normalizer.normalize("2125551234", "E164", "US"));
    }

    @Test
    void usNumber_toNational() {
        assertEquals("(212) 555-1234", normalizer.normalize("2125551234", "national", "US"));
    }

    @Test
    void usNumber_toNationalDash() {
        assertEquals("212-555-1234", normalizer.normalize("2125551234", "national_dash", "US"));
    }

    @Test
    void usNumber_toNationalDots() {
        assertEquals("212.555.1234", normalizer.normalize("2125551234", "national_dots", "US"));
    }

    @Test
    void usNumber_toDigits() {
        assertEquals("2125551234", normalizer.normalize("2125551234", "digits", "US"));
    }

    // ── Group 3: US input with explicit country code ──────────────────────────

    @Test
    void usInput_11DigitsLeading1_toE164() {
        // 11 digits starting with 1 — recognized as US with country code
        assertEquals("+12125551234", normalizer.normalize("12125551234", "E164", "US"));
    }

    @Test
    void usInput_explicitPlus_toNational() {
        assertEquals("(212) 555-1234", normalizer.normalize("+12125551234", "national", "US"));
    }

    @Test
    void usInput_dashedWithCountryCode_toNational() {
        assertEquals("(212) 555-1234", normalizer.normalize("1-212-555-1234", "national", "US"));
    }

    // ── Group 4: Extension stripping ─────────────────────────────────────────

    @Test
    void extension_x_isStripped() {
        assertEquals("(212) 555-1234", normalizer.normalize("212-555-1234 x123", "national", "US"));
    }

    @Test
    void extension_extDot_isStripped() {
        assertEquals("(212) 555-1234", normalizer.normalize("212-555-1234 ext. 456", "national", "US"));
    }

    @Test
    void extension_fullWord_isStripped() {
        assertEquals("(212) 555-1234", normalizer.normalize("212-555-1234 extension 789", "national", "US"));
    }

    // ── Group 5: US area code and exchange validation ─────────────────────────

    @Test
    void invalid_areaCodeStartsWithZero() {
        assertEquals("INVALID_PHONE: 012-555-1234 (invalid US area code)",
                normalizer.normalize("012-555-1234", "national", "US"));
    }

    @Test
    void invalid_areaCodeStartsWithOne() {
        assertEquals("INVALID_PHONE: 112-555-1234 (invalid US area code)",
                normalizer.normalize("112-555-1234", "national", "US"));
    }

    @Test
    void invalid_exchangeStartsWithZero() {
        assertEquals("INVALID_PHONE: 212-011-1234 (invalid US exchange)",
                normalizer.normalize("212-011-1234", "national", "US"));
    }

    @Test
    void invalid_exchangeStartsWithOne() {
        assertEquals("INVALID_PHONE: 212-155-1234 (invalid US exchange)",
                normalizer.normalize("212-155-1234", "national", "US"));
    }

    // ── Group 6: Length validation ────────────────────────────────────────────

    @Test
    void invalid_tooShort_us() {
        // 3 digits — under the 7-digit US minimum
        assertEquals("INVALID_PHONE: 123 (too short)",
                normalizer.normalize("123", "national", "US"));
    }

    @Test
    void invalid_tooLong_us() {
        // 12 digits without + — over the 11-digit US maximum
        assertEquals("INVALID_PHONE: 123456789012 (too long for US number)",
                normalizer.normalize("123456789012", "national", "US"));
    }

    @Test
    void invalid_tooLong_international() {
        // 16 digits with + — over the 15-digit E.164 maximum
        assertEquals("INVALID_PHONE: +1234567890123456 (too long)",
                normalizer.normalize("+1234567890123456", "E164", null));
    }

    // ── Group 7: All non-digit input ──────────────────────────────────────────

    @Test
    void invalid_allLetters_noDigits() {
        // digits string is empty after stripping → no suffix
        assertEquals("INVALID_PHONE: abc",
                normalizer.normalize("abc", "national", "US"));
    }

    // ── Group 8: International numbers (no defaultCountry) ───────────────────

    @Test
    void international_explicitPlus_toE164() {
        assertEquals("+442071234567", normalizer.normalize("+442071234567", "E164", null));
    }

    @Test
    void international_explicitPlus_toNationalDash_usesGenericFormatter() {
        // formatGeneric: rest=44207, mid3=123, last4=4567 → "44207-123-4567"
        assertEquals("44207-123-4567", normalizer.normalize("+442071234567", "national_dash", null));
    }

    @Test
    void international_explicitPlus_toNationalDots_usesGenericFormatter() {
        assertEquals("44207.123.4567", normalizer.normalize("+442071234567", "national_dots", null));
    }

    // ── Group 9: No defaultCountry — plain digits treated as raw ─────────────

    @Test
    void noDefaultCountry_rawDigits_toE164() {
        // No + and no defaultCountry → digits used as-is → "+" prepended
        assertEquals("+2125551234", normalizer.normalize("2125551234", "E164", null));
    }

    // ── Group 10: digits format strips leading 1 for US numbers ──────────────

    @Test
    void digits_format_stripsLeadingOne_forUS() {
        // 11-digit US number → "digits" format removes the leading country code
        assertEquals("2125551234", normalizer.normalize("12125551234", "digits", "US"));
    }

    // ── Group 11: Unknown format ──────────────────────────────────────────────

    @Test
    void invalid_unknownFormat() {
        assertEquals("INVALID_PHONE: 2125551234 (unknown format: badformat)",
                normalizer.normalize("2125551234", "badformat", "US"));
    }

    // ── Group 12: Null format defaults to "national" ──────────────────────────

    @Test
    void nullFormat_defaultsToNational() {
        assertEquals("(212) 555-1234", normalizer.normalize("2125551234", null, "US"));
    }

    // ── Group 13: Null / blank passthrough ────────────────────────────────────

    @Test
    void passthrough_null() {
        assertNull(normalizer.normalize(null, "national", "US"));
    }

    @Test
    void passthrough_emptyString() {
        assertEquals("", normalizer.normalize("", "national", "US"));
    }

    @Test
    void passthrough_blankString() {
        assertEquals("   ", normalizer.normalize("   ", "national", "US"));
    }
}