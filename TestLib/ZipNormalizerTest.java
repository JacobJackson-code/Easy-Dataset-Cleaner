import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ZipNormalizerTest {

    private ZipNormalizer normalizer;

    @BeforeEach
    void setUp() {
        normalizer = new ZipNormalizer();
    }

    // ── Group 1: US ZIP5 input → all formats ─────────────────────────────────

    @Test
    void usZip5_auto() {
        assertEquals("12345", normalizer.normalize("12345", "auto", "US"));
    }

    @Test
    void usZip5_zip5() {
        assertEquals("12345", normalizer.normalize("12345", "zip5", "US"));
    }

    @Test
    void usZip5_zip9_rejectsNoPlusFour() {
        // ZIP5 has no +4 data — can't satisfy zip9 request
        assertEquals("INVALID_ZIP: 12345 (no +4 code available)",
                normalizer.normalize("12345", "zip9", "US"));
    }

    @Test
    void usZip5_zip9Space_rejectsNoPlusFour() {
        assertEquals("INVALID_ZIP: 12345 (no +4 code available)",
                normalizer.normalize("12345", "zip9_space", "US"));
    }

    // ── Group 2: US ZIP+4 with separator → all formats ───────────────────────

    @Test
    void usZip9Dash_toZip9() {
        assertEquals("12345-6789", normalizer.normalize("12345-6789", "zip9", "US"));
    }

    @Test
    void usZip9Dash_toZip5() {
        assertEquals("12345", normalizer.normalize("12345-6789", "zip5", "US"));
    }

    @Test
    void usZip9Dash_toZip9Space() {
        assertEquals("12345 6789", normalizer.normalize("12345-6789", "zip9_space", "US"));
    }

    @Test
    void usZip9Dash_toAuto() {
        assertEquals("12345-6789", normalizer.normalize("12345-6789", "auto", "US"));
    }

    @Test
    void usZip9SpaceSeparator_toZip9() {
        // US_ZIP9 pattern accepts \s as separator in addition to -
        assertEquals("12345-6789", normalizer.normalize("12345 6789", "zip9", "US"));
    }

    // ── Group 3: US ZIP+4 bare (9 consecutive digits, no separator) ──────────

    @Test
    void usZip9Bare_toZip9() {
        assertEquals("12345-6789", normalizer.normalize("123456789", "zip9", "US"));
    }

    @Test
    void usZip9Bare_toZip5() {
        assertEquals("12345", normalizer.normalize("123456789", "zip5", "US"));
    }

    @Test
    void usZip9Bare_toZip9Space() {
        assertEquals("12345 6789", normalizer.normalize("123456789", "zip9_space", "US"));
    }

    @Test
    void usZip9Bare_toAuto() {
        assertEquals("12345-6789", normalizer.normalize("123456789", "auto", "US"));
    }

    // ── Group 4: US whitespace handling ──────────────────────────────────────

    @Test
    void us_leadingTrailingSpaces_validResult() {
        // trimmed before processing — valid zip still resolves
        assertEquals("12345", normalizer.normalize("  12345  ", "auto", "US"));
    }

    // ── Group 5: Canadian postal codes — valid inputs ─────────────────────────

    @Test
    void ca_standard_spaceFormat() {
        assertEquals("K1A 0A9", normalizer.normalize("K1A 0A9", "auto", "CA"));
    }

    @Test
    void ca_lowercase_isUppercased() {
        assertEquals("K1A 0A9", normalizer.normalize("k1a 0a9", "auto", "CA"));
    }

    @Test
    void ca_hyphenSeparator_normalizedToSpace() {
        // CA_POSTAL pattern accepts [\s\-]? — hyphen is valid input
        assertEquals("K1A 0A9", normalizer.normalize("K1A-0A9", "auto", "CA"));
    }

    @Test
    void ca_noSeparator_normalizedToSpace() {
        assertEquals("K1A 0A9", normalizer.normalize("K1A0A9", "auto", "CA"));
    }

    @Test
    void ca_differentCode() {
        assertEquals("V6B 4N7", normalizer.normalize("V6B 4N7", "auto", "CA"));
    }

    // ── Group 6: Canadian auto-detected from null country ─────────────────────

    @Test
    void ca_autoDetected_nullCountry() {
        assertEquals("K1A 0A9", normalizer.normalize("K1A 0A9", "auto", null));
    }

    // ── Group 7: UK postcodes — valid inputs ──────────────────────────────────

    @Test
    void uk_standardFormat() {
        assertEquals("SW1A 1AA", normalizer.normalize("SW1A 1AA", "auto", "UK"));
    }

    @Test
    void uk_lowercase_isUppercased() {
        assertEquals("SW1A 1AA", normalizer.normalize("sw1a 1aa", "auto", "UK"));
    }

    @Test
    void uk_noSpace_normalizedToSpace() {
        // UK_POSTAL pattern accepts optional [\s]? — no separator is valid
        assertEquals("SW1A 1AA", normalizer.normalize("SW1A1AA", "auto", "UK"));
    }

    @Test
    void uk_differentCode_EC1A() {
        assertEquals("EC1A 1BB", normalizer.normalize("EC1A 1BB", "auto", "UK"));
    }

    @Test
    void uk_singleLetterAreaCode() {
        // N1 9GU — outward code is 1 letter + 1 digit (no optional char)
        assertEquals("N1 9GU", normalizer.normalize("N1 9GU", "auto", "UK"));
    }

    // ── Group 8: UK auto-detected from null country ───────────────────────────

    @Test
    void uk_autoDetected_nullCountry() {
        assertEquals("SW1A 1AA", normalizer.normalize("SW1A 1AA", "auto", null));
    }

    // ── Group 9: country="GB" is accepted as alias for UK ────────────────────

    @Test
    void uk_countryGB_alias() {
        assertEquals("SW1A 1AA", normalizer.normalize("SW1A 1AA", "auto", "GB"));
    }

    // ── Group 10: US invalid inputs ───────────────────────────────────────────

    @Test
    void invalid_us_tooShort() {
        assertEquals("INVALID_ZIP: 1234 (too short)",
                normalizer.normalize("1234", "auto", "US"));
    }

    @Test
    void invalid_us_tooLong() {
        assertEquals("INVALID_ZIP: 1234567890 (too long)",
                normalizer.normalize("1234567890", "auto", "US"));
    }

    @Test
    void invalid_us_allLetters() {
        // Non-digit, non-dash input → cleaned to empty → general invalid
        assertEquals("INVALID_ZIP: ABCDE",
                normalizer.normalize("ABCDE", "auto", "US"));
    }

    // ── Group 11: Canadian invalid input (explicit country) ───────────────────

    @Test
    void invalid_ca_explicit() {
        assertEquals("INVALID_ZIP: AAAAA (invalid Canadian postal code)",
                normalizer.normalize("AAAAA", "auto", "CA"));
    }

    // ── Group 12: UK invalid inputs (explicit country) ────────────────────────

    @Test
    void invalid_uk_explicit() {
        assertEquals("INVALID_ZIP: NOTAZIP (invalid UK postcode)",
                normalizer.normalize("NOTAZIP", "auto", "UK"));
    }

    @Test
    void invalid_uk_hyphenNotAccepted() {
        // UK_POSTAL only allows [\s]? — hyphen is not a valid separator
        assertEquals("INVALID_ZIP: SW1A-1AA (invalid UK postcode)",
                normalizer.normalize("SW1A-1AA", "auto", "UK"));
    }

    // ── Group 13: Null format defaults to "auto" ──────────────────────────────

    @Test
    void nullFormat_defaultsToAuto() {
        assertEquals("12345", normalizer.normalize("12345", null, "US"));
    }

    // ── Group 14: Null / blank passthrough ────────────────────────────────────

    @Test
    void passthrough_null() {
        assertNull(normalizer.normalize(null, "auto", "US"));
    }

    @Test
    void passthrough_emptyString() {
        assertEquals("", normalizer.normalize("", "auto", "US"));
    }

    @Test
    void passthrough_blankString() {
        assertEquals("   ", normalizer.normalize("   ", "auto", "US"));
    }
}