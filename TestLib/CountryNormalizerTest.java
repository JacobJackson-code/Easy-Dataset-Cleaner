import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class CountryNormalizerTest {

    private CountryNormalizer normalizer;

    @BeforeEach
    void setUp() {
        normalizer = new CountryNormalizer();
    }

    // ── Group 1: ISO2 input → all three output formats ────────────────────────

    @Test
    void iso2_US_toIso2() {
        assertEquals("US", normalizer.normalize("US", "iso2"));
    }

    @Test
    void iso2_US_toIso3() {
        assertEquals("USA", normalizer.normalize("US", "iso3"));
    }

    @Test
    void iso2_US_toFullName() {
        assertEquals("United States", normalizer.normalize("US", "fullname"));
    }

    @Test
    void iso2_DE_toIso2() {
        assertEquals("DE", normalizer.normalize("DE", "iso2"));
    }

    @Test
    void iso2_DE_toIso3() {
        assertEquals("DEU", normalizer.normalize("DE", "iso3"));
    }

    @Test
    void iso2_DE_toFullName() {
        assertEquals("Germany", normalizer.normalize("DE", "fullname"));
    }

    @Test
    void iso2_JP_toFullName() {
        assertEquals("Japan", normalizer.normalize("JP", "fullname"));
    }

    // ── Group 2: ISO3 input → all three output formats ────────────────────────

    @Test
    void iso3_USA_toIso2() {
        assertEquals("US", normalizer.normalize("USA", "iso2"));
    }

    @Test
    void iso3_USA_toIso3() {
        assertEquals("USA", normalizer.normalize("USA", "iso3"));
    }

    @Test
    void iso3_FRA_toFullName() {
        assertEquals("France", normalizer.normalize("FRA", "fullname"));
    }

    @Test
    void iso3_DEU_toIso2() {
        assertEquals("DE", normalizer.normalize("DEU", "iso2"));
    }

    @Test
    void iso3_JPN_toIso3() {
        assertEquals("JPN", normalizer.normalize("JPN", "iso3"));
    }

    // ── Group 3: Full name input → all three output formats ───────────────────

    @Test
    void fullName_Germany_toIso2() {
        assertEquals("DE", normalizer.normalize("Germany", "iso2"));
    }

    @Test
    void fullName_Germany_toIso3() {
        assertEquals("DEU", normalizer.normalize("Germany", "iso3"));
    }

    @Test
    void fullName_Germany_toFullName() {
        assertEquals("Germany", normalizer.normalize("Germany", "fullname"));
    }

    @Test
    void fullName_France_toIso2() {
        assertEquals("FR", normalizer.normalize("France", "iso2"));
    }

    @Test
    void fullName_UnitedStates_toIso2() {
        assertEquals("US", normalizer.normalize("United States", "iso2"));
    }

    @Test
    void fullName_Japan_toIso3() {
        assertEquals("JPN", normalizer.normalize("Japan", "iso3"));
    }

    // ── Group 4: Informal / alternative names and aliases ─────────────────────

    @Test
    void alias_america_toIso2() {
        assertEquals("US", normalizer.normalize("america", "iso2"));
    }

    @Test
    void alias_unitedStatesOfAmerica_toIso2() {
        assertEquals("US", normalizer.normalize("united states of america", "iso2"));
    }

    @Test
    void alias_usa_dotted_toIso2() {
        assertEquals("US", normalizer.normalize("u.s.a.", "iso2"));
    }

    @Test
    void alias_us_partialDotted_toIso2() {
        // "u.s" (no trailing dot) registered as a variant
        assertEquals("US", normalizer.normalize("u.s", "iso2"));
    }

    @Test
    void alias_england_toIso2() {
        assertEquals("GB", normalizer.normalize("england", "iso2"));
    }

    @Test
    void alias_scotland_toIso2() {
        assertEquals("GB", normalizer.normalize("scotland", "iso2"));
    }

    @Test
    void alias_wales_toIso2() {
        assertEquals("GB", normalizer.normalize("wales", "iso2"));
    }

    @Test
    void alias_uk_toIso2() {
        assertEquals("GB", normalizer.normalize("uk", "iso2"));
    }

    @Test
    void alias_greatBritain_toFullName() {
        assertEquals("United Kingdom", normalizer.normalize("great britain", "fullname"));
    }

    @Test
    void alias_deutschland_toIso2() {
        assertEquals("DE", normalizer.normalize("deutschland", "iso2"));
    }

    @Test
    void alias_oz_toIso2() {
        assertEquals("AU", normalizer.normalize("oz", "iso2"));
    }

    @Test
    void alias_brasil_toIso2() {
        assertEquals("BR", normalizer.normalize("brasil", "iso2"));
    }

    @Test
    void alias_hellas_toIso2() {
        assertEquals("GR", normalizer.normalize("hellas", "iso2"));
    }

    @Test
    void alias_zaire_toFullName() {
        // Historical name for Democratic Republic of the Congo
        assertEquals("Democratic Republic of the Congo", normalizer.normalize("zaire", "fullname"));
    }

    @Test
    void alias_burma_toIso2() {
        assertEquals("MM", normalizer.normalize("burma", "iso2"));
    }

    @Test
    void alias_kampuchea_toIso2() {
        assertEquals("KH", normalizer.normalize("kampuchea", "iso2"));
    }

    @Test
    void alias_ceylon_toIso2() {
        assertEquals("LK", normalizer.normalize("ceylon", "iso2"));
    }

    @Test
    void alias_swaziland_toIso2() {
        assertEquals("SZ", normalizer.normalize("swaziland", "iso2"));
    }

    @Test
    void alias_theBahamas_toIso2() {
        assertEquals("BS", normalizer.normalize("the bahamas", "iso2"));
    }

    @Test
    void alias_theNetherlands_toIso2() {
        assertEquals("NL", normalizer.normalize("the netherlands", "iso2"));
    }

    @Test
    void alias_holland_toIso2() {
        assertEquals("NL", normalizer.normalize("holland", "iso2"));
    }

    @Test
    void alias_prc_toIso2() {
        assertEquals("CN", normalizer.normalize("prc", "iso2"));
    }

    @Test
    void alias_mainlandChina_toIso2() {
        assertEquals("CN", normalizer.normalize("mainland china", "iso2"));
    }

    @Test
    void alias_peoplesRepublicOfChina_apostrophe_toIso2() {
        assertEquals("CN", normalizer.normalize("people's republic of china", "iso2"));
    }

    @Test
    void alias_dprk_toIso2() {
        assertEquals("KP", normalizer.normalize("dprk", "iso2"));
    }

    @Test
    void alias_ksa_toIso2() {
        assertEquals("SA", normalizer.normalize("ksa", "iso2"));
    }

    @Test
    void alias_rsa_toIso2() {
        assertEquals("ZA", normalizer.normalize("rsa", "iso2"));
    }

    @Test
    void alias_drc_toIso2() {
        assertEquals("CD", normalizer.normalize("drc", "iso2"));
    }

    @Test
    void alias_czechia_toIso2() {
        assertEquals("CZ", normalizer.normalize("czechia", "iso2"));
    }

    @Test
    void alias_swiss_toIso2() {
        assertEquals("CH", normalizer.normalize("swiss", "iso2"));
    }

    @Test
    void alias_italia_toIso2() {
        assertEquals("IT", normalizer.normalize("italia", "iso2"));
    }

    @Test
    void alias_russia_toIso2() {
        assertEquals("RU", normalizer.normalize("russia", "iso2"));
    }

    @Test
    void alias_russianFederation_toIso3() {
        assertEquals("RUS", normalizer.normalize("russian federation", "iso3"));
    }

    @Test
    void alias_espana_unaccented_toIso2() {
        assertEquals("ES", normalizer.normalize("espana", "iso2"));
    }

    @Test
    void alias_espana_accented_toIso2() {
        // Unicode accented character registered as a variant
        assertEquals("ES", normalizer.normalize("españa", "iso2"));
    }

    @Test
    void alias_korea_unqualified_toIso2() {
        // "korea" alone maps to South Korea (KR)
        assertEquals("KR", normalizer.normalize("korea", "iso2"));
    }

    @Test
    void alias_northKorea_toIso2() {
        assertEquals("KP", normalizer.normalize("north korea", "iso2"));
    }

    @Test
    void alias_turkiye_unaccented_toIso2() {
        assertEquals("TR", normalizer.normalize("turkiye", "iso2"));
    }

    @Test
    void alias_antigua_partial_toIso2() {
        // "antigua" alone maps to Antigua and Barbuda
        assertEquals("AG", normalizer.normalize("antigua", "iso2"));
    }

    @Test
    void alias_guineaBissau_nohyphen_toIso2() {
        assertEquals("GW", normalizer.normalize("guinea bissau", "iso2"));
    }

    @Test
    void alias_trinidad_partial_toIso2() {
        // "trinidad" alone maps to Trinidad and Tobago
        assertEquals("TT", normalizer.normalize("trinidad", "iso2"));
    }

    @Test
    void alias_eastTimor_toIso2() {
        assertEquals("TL", normalizer.normalize("east timor", "iso2"));
    }

    // ── Group 5: Case insensitivity ───────────────────────────────────────────

    @Test
    void caseInsensitive_allUppercase() {
        assertEquals("DE", normalizer.normalize("GERMANY", "iso2"));
    }

    @Test
    void caseInsensitive_allLowercase() {
        assertEquals("DE", normalizer.normalize("germany", "iso2"));
    }

    @Test
    void caseInsensitive_mixedCase() {
        assertEquals("DE", normalizer.normalize("gErMaNy", "iso2"));
    }

    @Test
    void caseInsensitive_lowercaseIso2() {
        assertEquals("US", normalizer.normalize("usa", "iso2"));
    }

    @Test
    void caseInsensitive_lowercaseIso3() {
        assertEquals("FR", normalizer.normalize("fra", "iso2"));
    }

    // ── Group 6: Whitespace handling ──────────────────────────────────────────

    @Test
    void whitespace_leadingTrailing_iso2input() {
        assertEquals("US", normalizer.normalize("  US  ", "iso2"));
    }

    @Test
    void whitespace_leadingTrailing_fullNameInput() {
        assertEquals("DE", normalizer.normalize("  Germany  ", "iso2"));
    }

    // ── Group 7: Invalid inputs — flag string ─────────────────────────────────

    @Test
    void invalid_unknownCode_iso2format() {
        assertEquals("INVALID_COUNTRY: XYZ", normalizer.normalize("XYZ", "iso2"));
    }

    @Test
    void invalid_nonsense_iso2format() {
        assertEquals("INVALID_COUNTRY: notacountry", normalizer.normalize("notacountry", "iso2"));
    }

    @Test
    void invalid_unknownCode_iso3format() {
        assertEquals("INVALID_COUNTRY: XYZ", normalizer.normalize("XYZ", "iso3"));
    }

    @Test
    void invalid_unknownCode_fullnameFormat() {
        assertEquals("INVALID_COUNTRY: XYZ", normalizer.normalize("XYZ", "fullname"));
    }

    // ── Group 8: Null / blank passthrough ─────────────────────────────────────

    @Test
    void passthrough_null() {
        assertNull(normalizer.normalize(null, "iso2"));
    }

    @Test
    void passthrough_emptyString() {
        assertEquals("", normalizer.normalize("", "iso2"));
    }

    @Test
    void passthrough_blankString() {
        assertEquals("   ", normalizer.normalize("   ", "iso2"));
    }

    // ── Group 9: Unknown format passthrough ───────────────────────────────────

    @Test
    void unknownFormat_returnsInputUnchanged() {
        // Default case in switch returns raw input
        assertEquals("US", normalizer.normalize("US", "unknown"));
    }

    @Test
    void unknownFormat_emptyFormatString_returnsInputUnchanged() {
        assertEquals("Germany", normalizer.normalize("Germany", ""));
    }
}