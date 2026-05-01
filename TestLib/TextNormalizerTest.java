import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for TextNormalizer.
 *
 * All methods under test (applyCase, applyName, applyBool, applyId,
 * applyValidate) are package-private. Because neither TextNormalizer nor this
 * test class declares a package, both live in the default package and the test
 * can access them directly — no reflection or public wrapper needed.
 *
 * Array conventions used below:
 *   • applyCase, applyName, applyValidate  → receive toLower(parts) from
 *     NumberNormalizer. Tests pass already-lowercase arrays to match.
 *   • applyBool, applyId                   → receive original (un-lowercased)
 *     parts from NumberNormalizer. Tests preserve case to match production use,
 *     which matters for custom bool output values (e.g., "Y/N") and literal
 *     prefix/suffix strings in id rules.
 */
@DisplayName("TextNormalizer")
class TextNormalizerTest {

    private TextNormalizer tn;

    @BeforeEach
    void setUp() {
        tn = new TextNormalizer();
    }


    // =========================================================================
    // Group 13 — applyCase()
    // =========================================================================

    @Nested
    @DisplayName("applyCase()")
    class ApplyCase {

        @Test
        @DisplayName("upper — entire string uppercased")
        void upper() {
            assertEquals("HELLO WORLD",
                    tn.applyCase("hello world", new String[]{"case", "upper"}));
        }

        @Test
        @DisplayName("lower — entire string lowercased")
        void lower() {
            assertEquals("hello world",
                    tn.applyCase("HELLO WORLD", new String[]{"case", "lower"}));
        }

        @Test
        @DisplayName("title — first letter of each space-delimited word capitalised")
        void title() {
            assertEquals("Hello World",
                    tn.applyCase("hello world", new String[]{"case", "title"}));
        }

        @Test
        @DisplayName("sentence — only very first character uppercased; rest lowercased")
        void sentence() {
            assertEquals("Hello world. how are you",
                    tn.applyCase("HELLO WORLD. HOW ARE YOU", new String[]{"case", "sentence"}));
        }

        @Test
        @DisplayName("missing subtype (parts.length < 2) → pass-through")
        void missingSubtype() {
            assertEquals("hello",
                    tn.applyCase("hello", new String[]{"case"}));
        }

        @Test
        @DisplayName("unknown subtype → default branch → pass-through")
        void unknownSubtype() {
            assertEquals("hello",
                    tn.applyCase("hello", new String[]{"case", "bogus"}));
        }
    }


    // =========================================================================
    // Group 14 — applyName() : full mode
    // =========================================================================

    @Nested
    @DisplayName("applyName() — full mode")
    class ApplyNameFull {

        @Test
        @DisplayName("basic first last → capitalised")
        void basicFirstLast() {
            assertEquals("John Smith",
                    tn.applyName("john smith", new String[]{"name", "full"}));
        }

        @Test
        @DisplayName("Last, First comma format → reversed then capitalised")
        void commaReversal() {
            assertEquals("John Smith",
                    tn.applyName("Smith, John", new String[]{"name", "full"}));
        }

        @Test
        @DisplayName("title prefix (Dr.) stripped from token list")
        void titlePrefixStripped() {
            assertEquals("John Smith",
                    tn.applyName("Dr. John Smith", new String[]{"name", "full"}));
        }

        @Test
        @DisplayName("suffix (Jr) recognised and appended with correct casing")
        void suffixAppended() {
            assertEquals("John Smith Jr.",
                    tn.applyName("John Smith Jr", new String[]{"name", "full"}));
        }

        @Test
        @DisplayName("hyphenated last name — capitaliseNameToken applied to each part")
        void hyphenatedName() {
            assertEquals("Mary-Jane Watson",
                    tn.applyName("Mary-Jane Watson", new String[]{"name", "full"}));
        }

        @Test
        @DisplayName("O' prefix rule — O'Brien")
        void oApostrophePrefix() {
            assertEquals("O'Brien Patrick",
                    tn.applyName("o'brien patrick", new String[]{"name", "full"}));
        }

        @Test
        @DisplayName("Mc prefix rule — McAllister")
        void mcPrefix() {
            assertEquals("John McAllister",
                    tn.applyName("john mcallister", new String[]{"name", "full"}));
        }

        @Test
        @DisplayName("Mac + consonant → Mac rule applied — MacDonald")
        void macConsonant() {
            assertEquals("Rob MacDonald",
                    tn.applyName("rob macdonald", new String[]{"name", "full"}));
        }

        @Test
        @DisplayName("Mac + vowel → no Mac rule, plain capitalise — Macintosh")
        void macVowel() {
            // mac[^aeiou] does not match 'i', so capitalizeFirst is used instead
            assertEquals("Rob Macintosh",
                    tn.applyName("rob macintosh", new String[]{"name", "full"}));
        }

        @Test
        @DisplayName("compact initial (C.Lee) → split to C. Lee before tokenising")
        void compactInitialSplit() {
            assertEquals("C. Lee",
                    tn.applyName("C.Lee", new String[]{"name", "full"}));
        }

        @Test
        @DisplayName("single token → INVALID_NAME: <input> (single name only)")
        void singleToken() {
            assertEquals("INVALID_NAME: john (single name only)",
                    tn.applyName("john", new String[]{"name", "full"}));
        }

        @Test
        @DisplayName("all tokens are recognised prefixes → nameTokens empty → INVALID_NAME")
        void allPrefixes() {
            assertEquals("INVALID_NAME: Dr.",
                    tn.applyName("Dr.", new String[]{"name", "full"}));
        }
    }


    // =========================================================================
    // Group 15 — applyName() : other modes
    // =========================================================================

    @Nested
    @DisplayName("applyName() — other modes")
    class ApplyNameModes {

        @Test
        @DisplayName("first mode — returns cased first token only")
        void firstMode() {
            assertEquals("John",
                    tn.applyName("John Michael Smith", new String[]{"name", "first"}));
        }

        @Test
        @DisplayName("last mode — returns cased last token only")
        void lastMode() {
            assertEquals("Smith",
                    tn.applyName("John Michael Smith", new String[]{"name", "last"}));
        }

        @Test
        @DisplayName("initial_last mode — \"J. Smith\"")
        void initialLast() {
            assertEquals("J. Smith",
                    tn.applyName("John Smith", new String[]{"name", "initial_last"}));
        }

        @Test
        @DisplayName("initialnodot_last mode — \"J.Smith\"")
        void initialNodotLast() {
            assertEquals("J.Smith",
                    tn.applyName("John Smith", new String[]{"name", "initialnodot_last"}));
        }

        @Test
        @DisplayName("initial_last with single token → INVALID_NAME (need first and last)")
        void initialLastSingleToken() {
            assertEquals("INVALID_NAME: john (need first and last)",
                    tn.applyName("john", new String[]{"name", "initial_last"}));
        }

        @Test
        @DisplayName("initialnodot_last with single token → INVALID_NAME (need first and last)")
        void initialNodotLastSingleToken() {
            assertEquals("INVALID_NAME: john (need first and last)",
                    tn.applyName("john", new String[]{"name", "initialnodot_last"}));
        }
    }


    // =========================================================================
    // Group 16 — applyBool()
    // =========================================================================

    @Nested
    @DisplayName("applyBool()")
    class ApplyBool {

        // applyBool receives original (un-lowercased) parts. BOOL_MAP lookup
        // is case-insensitive (input.trim().toLowerCase()). Default output
        // format is "true"/"false".

        @Test
        @DisplayName("truthy: yes → true")
        void yes() {
            assertEquals("true", tn.applyBool("yes", new String[]{"bool"}));
        }

        @Test
        @DisplayName("truthy: 1 → true")
        void one() {
            assertEquals("true", tn.applyBool("1", new String[]{"bool"}));
        }

        @Test
        @DisplayName("falsy: no → false")
        void no() {
            assertEquals("false", tn.applyBool("no", new String[]{"bool"}));
        }

        @Test
        @DisplayName("falsy: 0 → false")
        void zero() {
            assertEquals("false", tn.applyBool("0", new String[]{"bool"}));
        }

        @Test
        @DisplayName("domain-specific falsy: pending → false")
        void pending() {
            assertEquals("false", tn.applyBool("pending", new String[]{"bool"}));
        }

        @Test
        @DisplayName("domain-specific falsy: cancelled → false")
        void cancelled() {
            assertEquals("false", tn.applyBool("cancelled", new String[]{"bool"}));
        }

        @Test
        @DisplayName("domain-specific falsy: void → false")
        void voidValue() {
            assertEquals("false", tn.applyBool("void", new String[]{"bool"}));
        }

        @Test
        @DisplayName("multi-word BOOL_MAP key: \"not found\" → false")
        void notFound() {
            assertEquals("false", tn.applyBool("not found", new String[]{"bool"}));
        }

        @Test
        @DisplayName("case-insensitive lookup: YES → true")
        void caseInsensitive() {
            assertEquals("true", tn.applyBool("YES", new String[]{"bool"}));
        }

        @Test
        @DisplayName("custom format Y/N — truthy value produces Y")
        void customFormatTruthy() {
            assertEquals("Y", tn.applyBool("yes", new String[]{"bool", "Y/N"}));
        }

        @Test
        @DisplayName("custom format Y/N — falsy value produces N")
        void customFormatFalsy() {
            assertEquals("N", tn.applyBool("no", new String[]{"bool", "Y/N"}));
        }

        @Test
        @DisplayName("custom format 1/0 — truthy")
        void customFormatOneTrue() {
            assertEquals("1", tn.applyBool("true", new String[]{"bool", "1/0"}));
        }

        @Test
        @DisplayName("custom format 1/0 — falsy")
        void customFormatOneZero() {
            assertEquals("0", tn.applyBool("false", new String[]{"bool", "1/0"}));
        }

        @Test
        @DisplayName("value not in BOOL_MAP → INVALID_BOOL: <input>")
        void unknownValue() {
            assertEquals("INVALID_BOOL: maybe", tn.applyBool("maybe", new String[]{"bool"}));
        }
    }


    // =========================================================================
    // Group 17 — applyId()
    // =========================================================================

    @Nested
    @DisplayName("applyId()")
    class ApplyId {

        // applyId receives original (un-lowercased) parts. Keyword matching
        // ("startswith", "endswith", "letter", "number") is case-insensitive
        // internally. Literal prefix/suffix values are compared case-insensitively.

        @Test
        @DisplayName("no constraints — returns trimmed input unchanged")
        void noConstraints() {
            assertEquals("ABC123",
                    tn.applyId("ABC123", new String[]{"id"}));
        }

        @Test
        @DisplayName("startsWith:letter — starts with letter → valid")
        void startsWithLetterPass() {
            assertEquals("ABC123",
                    tn.applyId("ABC123", new String[]{"id", "startsWith", "letter"}));
        }

        @Test
        @DisplayName("startsWith:letter — starts with digit → INVALID_ID")
        void startsWithLetterFail() {
            assertEquals("INVALID_ID: 123ABC (expected to start with a letter)",
                    tn.applyId("123ABC", new String[]{"id", "startsWith", "letter"}));
        }

        @Test
        @DisplayName("startsWith:number — starts with digit → valid")
        void startsWithNumberPass() {
            assertEquals("123ABC",
                    tn.applyId("123ABC", new String[]{"id", "startsWith", "number"}));
        }

        @Test
        @DisplayName("startsWith:number — starts with letter → INVALID_ID")
        void startsWithNumberFail() {
            assertEquals("INVALID_ID: ABC123 (expected to start with a number)",
                    tn.applyId("ABC123", new String[]{"id", "startsWith", "number"}));
        }

        @Test
        @DisplayName("startsWith:ABC (literal) — matches → valid")
        void startsWithLiteralPass() {
            assertEquals("ABC123",
                    tn.applyId("ABC123", new String[]{"id", "startsWith", "ABC"}));
        }

        @Test
        @DisplayName("startsWith:ABC (literal) — does not match → INVALID_ID")
        void startsWithLiteralFail() {
            assertEquals("INVALID_ID: XYZ123 (expected to start with 'ABC')",
                    tn.applyId("XYZ123", new String[]{"id", "startsWith", "ABC"}));
        }

        @Test
        @DisplayName("endsWith:number — ends with digit → valid")
        void endsWithNumberPass() {
            assertEquals("ABC123",
                    tn.applyId("ABC123", new String[]{"id", "endsWith", "number"}));
        }

        @Test
        @DisplayName("endsWith:letter — ends with digit → INVALID_ID")
        void endsWithLetterFail() {
            assertEquals("INVALID_ID: ABC123 (expected to end with a letter)",
                    tn.applyId("ABC123", new String[]{"id", "endsWith", "letter"}));
        }

        @Test
        @DisplayName("endsWith:letter — ends with letter → valid")
        void endsWithLetterPass() {
            assertEquals("ABC",
                    tn.applyId("ABC", new String[]{"id", "endsWith", "letter"}));
        }

        @Test
        @DisplayName("combined startsWith + endsWith — both constraints satisfied")
        void combinedConstraints() {
            assertEquals("ID-001",
                    tn.applyId("ID-001",
                            new String[]{"id", "startsWith", "ID", "endsWith", "number"}));
        }
    }


    // =========================================================================
    // Group 18 — applyValidate() : email
    // =========================================================================

    @Nested
    @DisplayName("applyValidate() — email")
    class ValidateEmail {

        @Test
        @DisplayName("valid email already lowercase → returned as-is")
        void validLowercase() {
            assertEquals("user@example.com",
                    tn.applyValidate("user@example.com", new String[]{"validate", "email"}));
        }

        @Test
        @DisplayName("mixed-case email → normalised to lowercase")
        void validMixedCase() {
            assertEquals("user@example.com",
                    tn.applyValidate("User@Example.COM", new String[]{"validate", "email"}));
        }

        @Test
        @DisplayName("spaces around @ and . → stripped during normalisation")
        void spacesAroundAtAndDot() {
            assertEquals("user@example.com",
                    tn.applyValidate("user @ example . com", new String[]{"validate", "email"}));
        }

        @Test
        @DisplayName("no @ symbol → INVALID_EMAIL")
        void noAtSign() {
            assertEquals("INVALID_EMAIL: notanemail",
                    tn.applyValidate("notanemail", new String[]{"validate", "email"}));
        }

        @Test
        @DisplayName("multiple @ symbols → INVALID_EMAIL")
        void multipleAtSigns() {
            assertEquals("INVALID_EMAIL: two@@example.com",
                    tn.applyValidate("two@@example.com", new String[]{"validate", "email"}));
        }

        @Test
        @DisplayName("empty local part → INVALID_EMAIL")
        void emptyLocal() {
            assertEquals("INVALID_EMAIL: @example.com",
                    tn.applyValidate("@example.com", new String[]{"validate", "email"}));
        }

        @Test
        @DisplayName("domain before last dot is empty (user@.com) → INVALID_EMAIL")
        void emptyDomainBeforeDot() {
            assertEquals("INVALID_EMAIL: user@.com",
                    tn.applyValidate("user@.com", new String[]{"validate", "email"}));
        }

        @Test
        @DisplayName("no dot in domain → INVALID_EMAIL")
        void noDotInDomain() {
            assertEquals("INVALID_EMAIL: user@nodot",
                    tn.applyValidate("user@nodot", new String[]{"validate", "email"}));
        }
    }


    // =========================================================================
    // Group 19 — applyValidate() : url
    // =========================================================================

    @Nested
    @DisplayName("applyValidate() — url")
    class ValidateUrl {

        @Test
        @DisplayName("full HTTPS with www → valid, returned lowercase")
        void httpsWithWww() {
            assertEquals("https://www.example.com",
                    tn.applyValidate("https://www.example.com", new String[]{"validate", "url"}));
        }

        @Test
        @DisplayName("HTTP without www → valid")
        void httpNoWww() {
            assertEquals("http://example.com",
                    tn.applyValidate("http://example.com", new String[]{"validate", "url"}));
        }

        @Test
        @DisplayName("no scheme, bare domain — host.contains(\".\") check still passes")
        void bareDomain() {
            assertEquals("example.com",
                    tn.applyValidate("example.com", new String[]{"validate", "url"}));
        }

        @Test
        @DisplayName("no dot in host → INVALID_URL")
        void noDot() {
            assertEquals("INVALID_URL: nodoturl",
                    tn.applyValidate("nodoturl", new String[]{"validate", "url"}));
        }
    }


    // =========================================================================
    // Group 20 — applyValidate() : any
    // =========================================================================

    @Nested
    @DisplayName("applyValidate() — any")
    class ValidateAny {

        @Test
        @DisplayName("valid email — email check passes first, returns email result")
        void validEmail() {
            assertEquals("user@example.com",
                    tn.applyValidate("user@example.com", new String[]{"validate", "any"}));
        }

        @Test
        @DisplayName("valid URL — email check fails, URL check passes, returns URL result")
        void validUrl() {
            assertEquals("https://example.com",
                    tn.applyValidate("https://example.com", new String[]{"validate", "any"}));
        }

        @Test
        @DisplayName("neither email nor URL → INVALID_CONTACT: <input>")
        void neither() {
            assertEquals("INVALID_CONTACT: notcontact",
                    tn.applyValidate("notcontact", new String[]{"validate", "any"}));
        }
    }
}
