import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for NumberNormalizer.
 *
 * Coverage:
 *   • apply() guard rails: null/empty input, null/empty rule, dirty-cell
 *     pass-through (all FLAG_PREFIXES), quote stripping, special non-finite
 *     values (∞, NaN, Infinity), unknown rule type pass-through
 *   • apply() parens notation: negStyle = flag / standard / parens
 *   • apply() cap enforcement: below / at / above cap; text handlers bypass cap
 *   • Numeric rules: integer, words, decimal, round, currency, percent
 *   • formatForCurrency(): per-currency style and decimal config, dirty-cell guard
 *   • currency_code rule (text handler path)
 *   • Scientific notation toggle (setAllowScientificNotation)
 *   • Smoke tests confirming delegation to TextNormalizer for all text-handler
 *     rule types; full coverage for those is in TextNormalizerTest.
 *
 * ── Known pending issue ────────────────────────────────────────────────────
 * The `currency` rule always applies 2 decimal places regardless of currency
 * code, because formatCurrency() hard-codes dp=2. JPY therefore produces
 * "¥1,234.50" instead of the intended "¥1,234". Tests for that rule are marked
 * with a comment and assert current (not ideal) behaviour so they will catch
 * any unintended change while the fix is pending.
 * ──────────────────────────────────────────────────────────────────────────
 */
@DisplayName("NumberNormalizer")
class NumberNormalizerTest {

    private NumberNormalizer nn;

    @BeforeEach
    void setUp() {
        nn = new NumberNormalizer();
    }


    // =========================================================================
    // Group 1 — apply() : Guard Rails
    // =========================================================================

    @Nested
    @DisplayName("apply() — Guard Rails")
    class GuardRails {

        @Test
        @DisplayName("null input returns null")
        void nullInput() {
            assertNull(nn.apply(null, "integer"));
        }

        @Test
        @DisplayName("empty string input returns empty string")
        void emptyInput() {
            assertEquals("", nn.apply("", "integer"));
        }

        @Test
        @DisplayName("whitespace-only input returns unchanged")
        void blankInput() {
            assertEquals("   ", nn.apply("   ", "integer"));
        }

        @Test
        @DisplayName("null rule returns input unchanged")
        void nullRule() {
            assertEquals("42", nn.apply("42", null));
        }

        @Test
        @DisplayName("empty rule returns input unchanged")
        void emptyRule() {
            assertEquals("42", nn.apply("42", ""));
        }

        // ── Dirty-cell pass-through (one case per distinct FLAG_PREFIXES entry) ──

        @Test
        @DisplayName("dirty: INVALID_NUMBER prefix — returns input unchanged")
        void dirtyInvalidNumber() {
            String dirty = "INVALID_NUMBER: abc";
            assertEquals(dirty, nn.apply(dirty, "integer"));
        }

        @Test
        @DisplayName("dirty: INVALID_BOOL prefix")
        void dirtyInvalidBool() {
            String dirty = "INVALID_BOOL: yes";
            assertEquals(dirty, nn.apply(dirty, "integer"));
        }

        @Test
        @DisplayName("dirty: INVALID_NAME prefix")
        void dirtyInvalidName() {
            String dirty = "INVALID_NAME: john (single name only)";
            assertEquals(dirty, nn.apply(dirty, "decimal"));
        }

        @Test
        @DisplayName("dirty: CAP_EXCEEDED prefix")
        void dirtyCapExceeded() {
            String dirty = "CAP_EXCEEDED: 1000";
            assertEquals(dirty, nn.apply(dirty, "integer"));
        }

        @Test
        @DisplayName("dirty: NULL_VALUE prefix")
        void dirtyNullValue() {
            String dirty = "NULL_VALUE: ";
            assertEquals(dirty, nn.apply(dirty, "integer"));
        }

        @Test
        @DisplayName("dirty: SUSPICIOUS_DATE prefix")
        void dirtySuspiciousDate() {
            String dirty = "SUSPICIOUS_DATE: 01/02/03";
            assertEquals(dirty, nn.apply(dirty, "decimal"));
        }

        @Test
        @DisplayName("dirty: REJECTED prefix")
        void dirtyRejected() {
            String dirty = "REJECTED: structural validation failed";
            assertEquals(dirty, nn.apply(dirty, "integer"));
        }

        // ── Quote stripping ──

        @Test
        @DisplayName("surrounding double-quotes stripped before processing")
        void quotesStripped() {
            assertEquals("42", nn.apply("\"42\"", "integer"));
        }

        @Test
        @DisplayName("quote-only input: after stripping trimmed is empty — returns original")
        void quotesOnlyReturnsOriginal() {
            assertEquals("\"\"", nn.apply("\"\"", "integer"));
        }

        // ── Special non-finite values ──

        @Test
        @DisplayName("∞ symbol → INVALID_NUMBER")
        void infinitySymbol() {
            assertEquals("INVALID_NUMBER: ∞", nn.apply("∞", "integer"));
        }

        @Test
        @DisplayName("-∞ symbol → INVALID_NUMBER")
        void negInfinitySymbol() {
            assertEquals("INVALID_NUMBER: -∞", nn.apply("-∞", "integer"));
        }

        @Test
        @DisplayName("\"NaN\" → INVALID_NUMBER")
        void nanString() {
            assertEquals("INVALID_NUMBER: NaN", nn.apply("NaN", "decimal"));
        }

        @Test
        @DisplayName("\"Infinity\" → INVALID_NUMBER")
        void infinityWord() {
            assertEquals("INVALID_NUMBER: Infinity", nn.apply("Infinity", "decimal"));
        }

        @Test
        @DisplayName("\"-infinity\" → INVALID_NUMBER")
        void negInfinityWord() {
            assertEquals("INVALID_NUMBER: -infinity", nn.apply("-infinity", "decimal"));
        }

        // ── Unknown rule ──

        @Test
        @DisplayName("unknown rule type → pass-through (no handler registered)")
        void unknownRule() {
            assertEquals("hello", nn.apply("hello", "foobar"));
        }
    }


    // =========================================================================
    // Group 2 — apply() : Parens Notation
    // =========================================================================

    @Nested
    @DisplayName("apply() — Parens Notation")
    class ParensNotation {

        @Test
        @DisplayName("parens input + default negStyle=flag → INVALID_NUMBER")
        void parensDefaultFlagStyle() {
            assertEquals("INVALID_NUMBER: (100)", nn.apply("(100)", "integer"));
        }

        @Test
        @DisplayName("parens input + negStyle=standard → converted to negative integer")
        void parensStandardStyle() {
            assertEquals("-100", nn.apply("(100)", "integer:standard"));
        }

        @Test
        @DisplayName("parens input + negStyle=parens → round-trip, output stays as parens")
        void parensRoundTrip() {
            // (100) → parensToNegative → -100 → toInteger → "-100" → applyParensOutput → (100)
            assertEquals("(100)", nn.apply("(100)", "integer:parens"));
        }

        @Test
        @DisplayName("negative plain input + negStyle=parens → wrapped in parens")
        void negativeToParens() {
            assertEquals("(50)", nn.apply("-50", "integer:parens"));
        }

        @Test
        @DisplayName("positive input + negStyle=parens → unchanged (only negatives are wrapped)")
        void positiveParensStyleUnchanged() {
            assertEquals("100", nn.apply("100", "integer:parens"));
        }
    }


    // =========================================================================
    // Group 3 — apply() : Cap Enforcement
    // =========================================================================

    @Nested
    @DisplayName("apply() — Cap Enforcement")
    class CapEnforcement {

        @Test
        @DisplayName("result below cap → normal output")
        void belowCap() {
            assertEquals("500", nn.apply("500", "integer", 1000L));
        }

        @Test
        @DisplayName("result exactly at cap → not exceeded (compareTo > 0, not >= 0)")
        void atCap() {
            assertEquals("1000", nn.apply("1000", "integer", 1000L));
        }

        @Test
        @DisplayName("result above cap → CAP_EXCEEDED: <result>")
        void aboveCap() {
            assertEquals("CAP_EXCEEDED: 1001", nn.apply("1001", "integer", 1000L));
        }

        @Test
        @DisplayName("text handler (case:upper) bypasses cap check entirely")
        void textHandlerBypassesCap() {
            // Text handlers return early before the cap block is reached.
            assertEquals("HELLO WORLD", nn.apply("hello world", "case:upper", 5L));
        }
    }


    // =========================================================================
    // Group 4 — Rule: integer
    // =========================================================================

    @Nested
    @DisplayName("Rule: integer")
    class IntegerRule {

        @Test
        @DisplayName("plain integer")
        void plainInteger() {
            assertEquals("42", nn.apply("42", "integer"));
        }

        @Test
        @DisplayName("negative integer")
        void negativeInteger() {
            assertEquals("-7", nn.apply("-7", "integer"));
        }

        @Test
        @DisplayName("decimal input truncates via longValue() — 42.9 → 42, not 43")
        void decimalTruncatesNotRounds() {
            assertEquals("42", nn.apply("42.9", "integer"));
        }

        @Test
        @DisplayName("comma-formatted input (thousands separator stripped)")
        void commaFormatted() {
            assertEquals("1234", nn.apply("1,234", "integer"));
        }

        @Test
        @DisplayName("currency-prefixed input ($1,234.56 → 1234, symbol+comma stripped, decimal truncated)")
        void currencyPrefixed() {
            assertEquals("1234", nn.apply("$1,234.56", "integer"));
        }

        @Test
        @DisplayName("word: single digit — \"one\" → 1")
        void wordSingleDigit() {
            assertEquals("1", nn.apply("one", "integer"));
        }

        @Test
        @DisplayName("word: hyphenated compound — \"twenty-five\" → 25")
        void wordHyphenatedCompound() {
            assertEquals("25", nn.apply("twenty-five", "integer"));
        }

        @Test
        @DisplayName("word: \"zero\" → 0 (special-cased to avoid false null)")
        void wordZero() {
            assertEquals("0", nn.apply("zero", "integer"));
        }

        @Test
        @DisplayName("word: multiplier — \"one thousand\" → 1000")
        void wordThousand() {
            assertEquals("1000", nn.apply("one thousand", "integer"));
        }

        @Test
        @DisplayName("word: full compound — \"one thousand two hundred thirty four\" → 1234")
        void wordFullCompound() {
            assertEquals("1234", nn.apply("one thousand two hundred thirty four", "integer"));
        }

        @Test
        @DisplayName("word: \"one million\" → 1000000")
        void wordMillion() {
            assertEquals("1000000", nn.apply("one million", "integer"));
        }

        @Test
        @DisplayName("non-numeric, non-word → INVALID_NUMBER")
        void invalidInput() {
            assertEquals("INVALID_NUMBER: abc", nn.apply("abc", "integer"));
        }
    }


    // =========================================================================
    // Group 5 — Rule: words
    // =========================================================================

    @Nested
    @DisplayName("Rule: words")
    class WordsRule {

        @Test
        @DisplayName("0 → Zero")
        void zero() {
            assertEquals("Zero", nn.apply("0", "words"));
        }

        @Test
        @DisplayName("5 → Five")
        void singleDigit() {
            assertEquals("Five", nn.apply("5", "words"));
        }

        @Test
        @DisplayName("21 → Twenty-One (two-digit compound with hyphen)")
        void twoDigitCompound() {
            assertEquals("Twenty-One", nn.apply("21", "words"));
        }

        @Test
        @DisplayName("100 → One Hundred")
        void hundred() {
            assertEquals("One Hundred", nn.apply("100", "words"));
        }

        @Test
        @DisplayName("1000 → One Thousand")
        void thousand() {
            assertEquals("One Thousand", nn.apply("1000", "words"));
        }

        @Test
        @DisplayName("1234 → One Thousand Two Hundred Thirty-Four")
        void fullCompound() {
            assertEquals("One Thousand Two Hundred Thirty-Four", nn.apply("1234", "words"));
        }

        @Test
        @DisplayName("1000000 → One Million")
        void million() {
            assertEquals("One Million", nn.apply("1000000", "words"));
        }

        @Test
        @DisplayName("negative → Negative prefix")
        void negative() {
            assertEquals("Negative Seven", nn.apply("-7", "words"));
        }

        @Test
        @DisplayName("non-numeric → INVALID_NUMBER (flagged inside toWords, not null-path)")
        void invalidInput() {
            assertEquals("INVALID_NUMBER: abc", nn.apply("abc", "words"));
        }
    }


    // =========================================================================
    // Group 6 — Rule: decimal
    // =========================================================================

    @Nested
    @DisplayName("Rule: decimal")
    class DecimalRule {

        @Test
        @DisplayName("default: 2 places, thousands comma")
        void defaultTwoPlaces() {
            assertEquals("42.50", nn.apply("42.5", "decimal"));
        }

        @Test
        @DisplayName("whole number gets decimal places added")
        void wholeNumberGetsDecimals() {
            assertEquals("42.00", nn.apply("42", "decimal"));
        }

        @Test
        @DisplayName("rounds to 2 places and applies thousands comma")
        void roundsAndComma() {
            assertEquals("1,234.57", nn.apply("1234.567", "decimal"));
        }

        @Test
        @DisplayName("custom place count: decimal:3")
        void customPlaces() {
            assertEquals("1,234.567", nn.apply("1234.567", "decimal:3"));
        }

        @Test
        @DisplayName("custom places + nocomma: decimal:3:nocomma")
        void customPlacesNoComma() {
            assertEquals("1234.567", nn.apply("1234.567", "decimal:3:nocomma"));
        }

        @Test
        @DisplayName("nocomma only — places default to 2: decimal:nocomma")
        void noCommaDefaultPlaces() {
            assertEquals("1234.57", nn.apply("1234.567", "decimal:nocomma"));
        }

        @Test
        @DisplayName("negative decimal")
        void negativeDecimal() {
            assertEquals("-3.14", nn.apply("-3.14", "decimal"));
        }

        @Test
        @DisplayName("European-format input (comma as decimal separator): 1234,50 → 1,234.50")
        void europeanDecimalInput() {
            assertEquals("1,234.50", nn.apply("1234,50", "decimal"));
        }

        @Test
        @DisplayName("European input with period=thousands + comma=decimal: 1.234,50 → 1,234.50")
        void europeanThousandsAndDecimal() {
            assertEquals("1,234.50", nn.apply("1.234,50", "decimal"));
        }

        @Test
        @DisplayName("Swiss apostrophe thousands separator: 1'234.50 → 1,234.50")
        void swissApostrophe() {
            assertEquals("1,234.50", nn.apply("1'234.50", "decimal"));
        }

        @Test
        @DisplayName("non-numeric → INVALID_NUMBER")
        void invalidInput() {
            assertEquals("INVALID_NUMBER: abc", nn.apply("abc", "decimal"));
        }
    }


    // =========================================================================
    // Group 7 — Rule: round
    // =========================================================================

    @Nested
    @DisplayName("Rule: round")
    class RoundRule {

        @Test
        @DisplayName("default nearest: 3.5 → 4  (HALF_UP)")
        void defaultNearestUp() {
            assertEquals("4", nn.apply("3.5", "round"));
        }

        @Test
        @DisplayName("default nearest: 3.4 → 3")
        void defaultNearestDown() {
            assertEquals("3", nn.apply("3.4", "round"));
        }

        @Test
        @DisplayName("round:up — ceiling: 3.1 → 4")
        void roundUp() {
            assertEquals("4", nn.apply("3.1", "round:up"));
        }

        @Test
        @DisplayName("round:down — floor: 3.9 → 3")
        void roundDown() {
            assertEquals("3", nn.apply("3.9", "round:down"));
        }

        @Test
        @DisplayName("thousands comma applied to rounded result")
        void thousandsCommaOnResult() {
            assertEquals("1,235", nn.apply("1234.5", "round"));
        }

        @Test
        @DisplayName("round:nocomma — nocomma token hits default direction (nearest), no comma")
        void noComma() {
            // parts[1]="nocomma" hits the default switch branch → HALF_UP; useCommas=false
            assertEquals("1235", nn.apply("1234.5", "round:nocomma"));
        }

        @Test
        @DisplayName("non-numeric → INVALID_NUMBER")
        void invalidInput() {
            assertEquals("INVALID_NUMBER: abc", nn.apply("abc", "round"));
        }
    }


    // =========================================================================
    // Group 8 — Rule: currency
    // =========================================================================

    @Nested
    @DisplayName("Rule: currency")
    class CurrencyRule {

        // NOTE: formatCurrency() hard-codes 2 decimal places and does not consult
        // CURRENCY_DECIMALS. JPY via this rule therefore produces "¥1,234.50" rather
        // than the intended "¥1,234". Tests assert current behaviour.
        // Tracked as a known pending issue — do not change expected values here
        // until the rule itself is fixed.

        @Test
        @DisplayName("default (USD): $ symbol, 2 dp, thousands comma")
        void defaultUsd() {
            assertEquals("$1,234.50", nn.apply("1234.5", "currency"));
        }

        @Test
        @DisplayName("explicit EUR symbol")
        void explicitEur() {
            assertEquals("€1,234.50", nn.apply("1234.5", "currency:eur"));
        }

        @Test
        @DisplayName("explicit GBP symbol")
        void explicitGbp() {
            assertEquals("£1,234.50", nn.apply("1234.5", "currency:gbp"));
        }

        @Test
        @DisplayName("JPY: always 2 dp via this rule (PENDING FIX — should be 0 dp)")
        void jpyAlwaysTwoDecimals() {
            assertEquals("¥1,234.50", nn.apply("1234.5", "currency:jpy"));
        }

        @Test
        @DisplayName("nocomma modifier: currency:usd:nocomma")
        void noComma() {
            assertEquals("$1234.50", nn.apply("1234.5", "currency:usd:nocomma"));
        }

        @Test
        @DisplayName("non-numeric → INVALID_NUMBER")
        void invalidInput() {
            assertEquals("INVALID_NUMBER: abc", nn.apply("abc", "currency"));
        }
    }


    // =========================================================================
    // Group 9 — Rule: percent
    // =========================================================================

    @Nested
    @DisplayName("Rule: percent")
    class PercentRule {

        // NOTE: formatPercent does NOT multiply by 100. It strips any existing %,
        // formats the number, then appends %. So 42.5 → "42.50%", not "50.00%".

        @Test
        @DisplayName("default: 2 decimal places, % appended")
        void defaultTwoPlaces() {
            assertEquals("42.50%", nn.apply("42.5", "percent"));
        }

        @Test
        @DisplayName("input already contains % sign — stripped before parsing, re-appended")
        void percentSignInInput() {
            assertEquals("42.50%", nn.apply("42.5%", "percent"));
        }

        @Test
        @DisplayName("thousands comma applied before %")
        void thousandsComma() {
            assertEquals("1,234.50%", nn.apply("1234.5", "percent"));
        }

        @Test
        @DisplayName("custom 1 decimal place: percent:1")
        void oneDecimalPlace() {
            assertEquals("42.5%", nn.apply("42.5", "percent:1"));
        }

        @Test
        @DisplayName("0 decimal places rounds the value: percent:0")
        void zeroPlaces() {
            assertEquals("43%", nn.apply("42.5", "percent:0"));
        }

        @Test
        @DisplayName("nocomma modifier: percent:nocomma")
        void noComma() {
            assertEquals("1234.50%", nn.apply("1234.5", "percent:nocomma"));
        }

        @Test
        @DisplayName("non-numeric → INVALID_NUMBER")
        void invalidInput() {
            assertEquals("INVALID_NUMBER: abc", nn.apply("abc", "percent"));
        }
    }


    // =========================================================================
    // Group 10 — formatForCurrency()
    // =========================================================================

    @Nested
    @DisplayName("formatForCurrency()")
    class FormatForCurrency {

        @Test
        @DisplayName("USD — STANDARD style, 2 dp, thousands comma")
        void usdDefault() {
            assertEquals("1,234.50", nn.formatForCurrency("1234.5", "USD", "default"));
        }

        @Test
        @DisplayName("EUR — EUROPEAN style: period=thousands, comma=decimal")
        void eurDefault() {
            assertEquals("1.234,50", nn.formatForCurrency("1234.5", "EUR", "default"));
        }

        @Test
        @DisplayName("CHF — SWISS style: apostrophe=thousands, period=decimal")
        void chfDefault() {
            assertEquals("1'234.50", nn.formatForCurrency("1234.5", "CHF", "default"));
        }

        @Test
        @DisplayName("JPY — 0 decimal places (CURRENCY_DECIMALS), rounds HALF_UP")
        void jpyDefault() {
            assertEquals("1,235", nn.formatForCurrency("1234.5", "JPY", "default"));
        }

        @Test
        @DisplayName("INR — INDIAN grouping (xx,xx,xxx.xx)")
        void inrDefault() {
            assertEquals("12,34,567.50", nn.formatForCurrency("1234567.5", "INR", "default"));
        }

        @Test
        @DisplayName("KWD — 3 decimal places (CURRENCY_DECIMALS)")
        void kwdDefault() {
            assertEquals("1,234.500", nn.formatForCurrency("1234.5", "KWD", "default"));
        }

        @Test
        @DisplayName("USD decimal mode — plain decimal string, no grouping separator")
        void usdDecimalMode() {
            assertEquals("1234.50", nn.formatForCurrency("1234.5", "USD", "decimal"));
        }

        @Test
        @DisplayName("JPY decimal mode — 0 dp, no decimal point in output")
        void jpyDecimalMode() {
            assertEquals("1235", nn.formatForCurrency("1234.5", "JPY", "decimal"));
        }

        @Test
        @DisplayName("EUR nogrouping mode — no period thousands, comma decimal only")
        void eurNoGrouping() {
            assertEquals("1234,50", nn.formatForCurrency("1234.5", "EUR", "nogrouping"));
        }

        @Test
        @DisplayName("dirty cell (INVALID_ prefix) — local isDirty() guard, returns input unchanged")
        void dirtyPassThrough() {
            assertEquals("INVALID_NUMBER: 5",
                    nn.formatForCurrency("INVALID_NUMBER: 5", "USD", "default"));
        }

        @Test
        @DisplayName("non-numeric input → INVALID_NUMBER")
        void invalidInput() {
            assertEquals("INVALID_NUMBER: abc",
                    nn.formatForCurrency("abc", "USD", "default"));
        }
    }


    // =========================================================================
    // Group 11 — Rule: currency_code   (text handler path)
    // =========================================================================

    @Nested
    @DisplayName("Rule: currency_code")
    class CurrencyCodeRule {

        @Test
        @DisplayName("known code, upper mode — usd → USD")
        void upper() {
            assertEquals("USD", nn.apply("usd", "currency_code:upper"));
        }

        @Test
        @DisplayName("known code, lower mode — EUR → eur")
        void lower() {
            assertEquals("eur", nn.apply("EUR", "currency_code:lower"));
        }

        @Test
        @DisplayName("known code, symbol mode — USD → $")
        void symbolUsd() {
            assertEquals("$", nn.apply("usd", "currency_code:symbol"));
        }

        @Test
        @DisplayName("known code, symbol mode — EUR → €")
        void symbolEur() {
            assertEquals("€", nn.apply("eur", "currency_code:symbol"));
        }

        @Test
        @DisplayName("CHF symbol has trailing space: \"CHF \" — pending review")
        void symbolChf() {
            // CURRENCY_SYMBOLS.put("CHF","CHF ") — trailing space is in the source data.
            assertEquals("CHF ", nn.apply("chf", "currency_code:symbol"));
        }

        @Test
        @DisplayName("unknown code → INVALID_CURRENCY")
        void unknownCode() {
            assertEquals("INVALID_CURRENCY: xyz", nn.apply("xyz", "currency_code:upper"));
        }

        @Test
        @DisplayName("no mode token (parts.length < 2) → pass-through")
        void noModeToken() {
            assertEquals("USD", nn.apply("USD", "currency_code"));
        }
    }


    // =========================================================================
    // Group 12 — Scientific Notation Toggle
    // =========================================================================

    @Nested
    @DisplayName("Scientific Notation Toggle")
    class ScientificNotation {

        @Test
        @DisplayName("blocked by default — 1e5 → INVALID_NUMBER")
        void blockedByDefault() {
            assertEquals("INVALID_NUMBER: 1e5", nn.apply("1e5", "decimal"));
        }

        @Test
        @DisplayName("enabled via setter — 1e5 parsed and formatted as decimal")
        void enabledParsesCorrectly() {
            nn.setAllowScientificNotation(true);
            assertEquals("100,000.00", nn.apply("1e5", "decimal"));
        }

        @Test
        @DisplayName("enabled — 1.5e3 parsed and formatted")
        void enabledCompoundExponent() {
            nn.setAllowScientificNotation(true);
            assertEquals("1,500.00", nn.apply("1.5e3", "decimal"));
        }
    }


    // =========================================================================
    // Group 13 — TextNormalizer Delegation Smoke Tests
    //
    // Confirms apply() correctly routes all text-handler rule types to
    // TextNormalizer. One representative case per handler is sufficient here;
    // exhaustive coverage lives in TextNormalizerTest.
    // =========================================================================

    @Nested
    @DisplayName("apply() — TextNormalizer delegation (smoke tests)")
    class TextDelegationSmoke {

        @Test
        @DisplayName("case rule routes to applyCase")
        void caseDelegate() {
            assertEquals("HELLO", nn.apply("hello", "case:upper"));
        }

        @Test
        @DisplayName("name rule routes to applyName")
        void nameDelegate() {
            assertEquals("John Smith", nn.apply("john smith", "name:full"));
        }

        @Test
        @DisplayName("bool rule routes to applyBool")
        void boolDelegate() {
            assertEquals("true", nn.apply("yes", "bool"));
        }

        @Test
        @DisplayName("id rule routes to applyId")
        void idDelegate() {
            assertEquals("ABC123", nn.apply("ABC123", "id:startsWith:letter"));
        }

        @Test
        @DisplayName("validate rule routes to applyValidate")
        void validateDelegate() {
            assertEquals("user@example.com", nn.apply("user@example.com", "validate:email"));
        }

        @Test
        @DisplayName("currency_code rule routes to applyCurrencyCode")
        void currencyCodeDelegate() {
            assertEquals("USD", nn.apply("usd", "currency_code:upper"));
        }
    }
}
