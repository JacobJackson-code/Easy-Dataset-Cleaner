import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class StateNormalizerTest {

    private StateNormalizer normalizer;

    @BeforeEach
    void setUp() {
        normalizer = new StateNormalizer();
    }

    // ── Group 1: Full name input → all three output formats ──────────────────

    @Test
    void fullName_toAbbr_california() {
        assertEquals("CA", normalizer.normalize("California", "abbr"));
    }

    @Test
    void fullName_toFull_california() {
        assertEquals("California", normalizer.normalize("California", "full"));
    }

    @Test
    void fullName_toAbbrDot_california() {
        assertEquals("C.A.", normalizer.normalize("California", "abbr_dot"));
    }

    @Test
    void fullName_toAbbr_newYork() {
        assertEquals("NY", normalizer.normalize("New York", "abbr"));
    }

    @Test
    void fullName_toFull_newYork() {
        assertEquals("New York", normalizer.normalize("New York", "full"));
    }

    @Test
    void fullName_toAbbr_texas() {
        assertEquals("TX", normalizer.normalize("Texas", "abbr"));
    }

    @Test
    void fullName_toAbbrDot_florida() {
        assertEquals("F.L.", normalizer.normalize("Florida", "abbr_dot"));
    }

    @Test
    void fullName_toAbbr_westVirginia() {
        assertEquals("WV", normalizer.normalize("West Virginia", "abbr"));
    }

    // ── Group 2: Abbreviation input → all three output formats ───────────────

    @Test
    void abbr_toAbbr_ca() {
        assertEquals("CA", normalizer.normalize("CA", "abbr"));
    }

    @Test
    void abbr_toFull_ca() {
        assertEquals("California", normalizer.normalize("CA", "full"));
    }

    @Test
    void abbr_toAbbrDot_ca() {
        assertEquals("C.A.", normalizer.normalize("CA", "abbr_dot"));
    }

    @Test
    void abbr_toFull_ny() {
        assertEquals("New York", normalizer.normalize("NY", "full"));
    }

    @Test
    void abbr_toAbbrDot_tx() {
        assertEquals("T.X.", normalizer.normalize("TX", "abbr_dot"));
    }

    @Test
    void abbr_toFull_fl() {
        assertEquals("Florida", normalizer.normalize("FL", "full"));
    }

    // ── Group 3: Common variant inputs ───────────────────────────────────────

    @Test
    void variant_cali_toAbbr() {
        assertEquals("CA", normalizer.normalize("Cali", "abbr"));
    }

    @Test
    void variant_calif_toAbbr() {
        assertEquals("CA", normalizer.normalize("Calif", "abbr"));
    }

    @Test
    void variant_cal_toAbbr() {
        assertEquals("CA", normalizer.normalize("Cal", "abbr"));
    }

    @Test
    void variant_goldenState_toAbbr() {
        assertEquals("CA", normalizer.normalize("golden state", "abbr"));
    }

    @Test
    void variant_tex_toAbbr() {
        assertEquals("TX", normalizer.normalize("Tex", "abbr"));
    }

    @Test
    void variant_tenn_toAbbr() {
        assertEquals("TN", normalizer.normalize("Tenn", "abbr"));
    }

    @Test
    void variant_fla_toAbbr() {
        assertEquals("FL", normalizer.normalize("Fla", "abbr"));
    }

    @Test
    void variant_ill_toAbbr() {
        assertEquals("IL", normalizer.normalize("Ill", "abbr"));
    }

    @Test
    void variant_penn_toAbbr() {
        assertEquals("PA", normalizer.normalize("Penn", "abbr"));
    }

    @Test
    void variant_mass_toAbbr() {
        assertEquals("MA", normalizer.normalize("Mass", "abbr"));
    }

    @Test
    void variant_mich_toAbbr() {
        assertEquals("MI", normalizer.normalize("Mich", "abbr"));
    }

    @Test
    void variant_geo_toAbbr() {
        assertEquals("GA", normalizer.normalize("Geo", "abbr"));
    }

    @Test
    void variant_ariz_toFull() {
        assertEquals("Arizona", normalizer.normalize("Ariz", "full"));
    }

    // ── Group 4: Dotted-form input ────────────────────────────────────────────
    // SN-28 to SN-31: direct map hits (variants stored with dots in static block)
    // SN-32 to SN-35: dot-strip fallback (dots not stored, stripped at runtime)

    @Test
    void dottedForm_NH_directHit_toAbbr() {
        // "n.h." is registered directly in TO_ABBR as a variant
        assertEquals("NH", normalizer.normalize("N.H.", "abbr"));
    }

    @Test
    void dottedForm_NJ_directHit_toAbbr() {
        assertEquals("NJ", normalizer.normalize("N.J.", "abbr"));
    }

    @Test
    void dottedForm_WV_directHit_toAbbr() {
        // "w.v." is registered directly in TO_ABBR as a variant
        assertEquals("WV", normalizer.normalize("W.V.", "abbr"));
    }

    @Test
    void dottedForm_RI_directHit_toFull() {
        assertEquals("Rhode Island", normalizer.normalize("R.I.", "full"));
    }

    @Test
    void dottedForm_CA_stripFallback_toAbbr() {
        // "c.a." is NOT stored as a variant; dot-stripping fallback handles it
        assertEquals("CA", normalizer.normalize("C.A.", "abbr"));
    }

    @Test
    void dottedForm_CA_stripFallback_toFull() {
        assertEquals("California", normalizer.normalize("C.A.", "full"));
    }

    @Test
    void dottedForm_FL_stripFallback_toAbbr() {
        assertEquals("FL", normalizer.normalize("F.L.", "abbr"));
    }

    @Test
    void dottedForm_NY_stripFallback_toAbbr() {
        assertEquals("NY", normalizer.normalize("N.Y.", "abbr"));
    }

    // ── Group 5: DC / District of Columbia ───────────────────────────────────

    @Test
    void dc_abbr_toFull() {
        assertEquals("District of Columbia", normalizer.normalize("DC", "full"));
    }

    @Test
    void dc_fullName_toAbbr() {
        assertEquals("DC", normalizer.normalize("District of Columbia", "abbr"));
    }

    @Test
    void dc_washingtonDC_toAbbr() {
        assertEquals("DC", normalizer.normalize("Washington DC", "abbr"));
    }

    @Test
    void dc_washingtonDotC_toAbbr() {
        // "washington d.c." registered as a variant for DC
        assertEquals("DC", normalizer.normalize("Washington D.C.", "abbr"));
    }

    @Test
    void dc_dottedAbbr_toAbbr() {
        assertEquals("DC", normalizer.normalize("D.C.", "abbr"));
    }

    // ── Group 6: Case insensitivity ───────────────────────────────────────────

    @Test
    void caseInsensitive_lowercase() {
        assertEquals("CA", normalizer.normalize("california", "abbr"));
    }

    @Test
    void caseInsensitive_uppercase() {
        assertEquals("CA", normalizer.normalize("CALIFORNIA", "abbr"));
    }

    @Test
    void caseInsensitive_mixedCase() {
        assertEquals("California", normalizer.normalize("cAlIfOrNiA", "full"));
    }

    @Test
    void caseInsensitive_lowercaseAbbr() {
        assertEquals("California", normalizer.normalize("ca", "full"));
    }

    // ── Group 7: Whitespace handling ──────────────────────────────────────────

    @Test
    void whitespace_leadingTrailing_abbr() {
        assertEquals("CA", normalizer.normalize("  CA  ", "abbr"));
    }

    @Test
    void whitespace_leadingTrailing_fullName() {
        assertEquals("CA", normalizer.normalize("  California  ", "abbr"));
    }

    @Test
    void whitespace_doubleSpaceInName() {
        // "New  York" → replaceAll("\\s+", " ") → "new york" → NY
        assertEquals("NY", normalizer.normalize("New  York", "abbr"));
    }

    // ── Group 8: Invalid inputs — flag string ─────────────────────────────────

    @Test
    void invalid_unknownAbbr_abbr() {
        assertEquals("INVALID_STATE: XX", normalizer.normalize("XX", "abbr"));
    }

    @Test
    void invalid_nonsense_abbr() {
        assertEquals("INVALID_STATE: notastate", normalizer.normalize("notastate", "abbr"));
    }

    @Test
    void invalid_unknownAbbr_full() {
        assertEquals("INVALID_STATE: ZZ", normalizer.normalize("ZZ", "full"));
    }

    @Test
    void invalid_withSurroundingSpaces_flagTrimmed() {
        // normalize() uses input.trim() in the flag string — spaces stripped
        assertEquals("INVALID_STATE: XX", normalizer.normalize("  XX  ", "abbr"));
    }

    // ── Group 9: Null / blank passthrough ─────────────────────────────────────

    @Test
    void passthrough_null() {
        assertNull(normalizer.normalize(null, "abbr"));
    }

    @Test
    void passthrough_emptyString() {
        assertEquals("", normalizer.normalize("", "abbr"));
    }

    @Test
    void passthrough_blankString() {
        assertEquals("   ", normalizer.normalize("   ", "abbr"));
    }
}
