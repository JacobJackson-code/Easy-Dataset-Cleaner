import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Single source of truth for every flag prefix used in the pipeline.
 *
 * MAINTENANCE RULE: adding or renaming a flag type means ONE edit here.
 * DataCleaner.isDirty() and ReportBuilder both reference these constants
 * at runtime — no manual sync required between files.
 *
 * Referenced by:
 *   DataCleaner  — isDirty() uses FLAG_PREFIXES
 *   ReportBuilder — extractFlagReport uses FLAG_PREFIXES and SHORT_TAGS
 */
public final class FlagConstants {

    private FlagConstants() {} // static-only class, never instantiated

    // ── All flag prefixes (used to identify a "dirty" cell) ───────────────────
    // Order does not matter for correctness; keep alphabetical for readability.
    public static final String[] FLAG_PREFIXES = {
            "CAP_EXCEEDED:",
            "INVALID_BOOL:",
            "INVALID_CONTACT:",
            "INVALID_CURRENCY:",
            "INVALID_DATE:",
            "INVALID_EMAIL:",
            "INVALID_ID:",
            "INVALID_NAME:",
            "INVALID_NUMBER:",
            "INVALID_PHONE:",
            "INVALID_STATE:",
            "INVALID_TIME:",
            "INVALID_URL:",
            "INVALID_ZIP:",
            "NULL_VALUE:",
            "REJECTED:",
            "SUSPICIOUS_DATE:",
    };

    // ── Short display tags shown in clean output when showFlagsInOutput=true ──
    // Each entry maps prefix → bracketed label.
    public static final Map<String, String> SHORT_TAGS = new LinkedHashMap<>();
    static {
        SHORT_TAGS.put("CAP_EXCEEDED:",     "[CAP_EXCEEDED]");
        SHORT_TAGS.put("INVALID_BOOL:",     "[INVALID_BOOL]");
        SHORT_TAGS.put("INVALID_CONTACT:",  "[INVALID_CONTACT]");
        SHORT_TAGS.put("INVALID_CURRENCY:", "[INVALID_CURRENCY]");
        SHORT_TAGS.put("INVALID_DATE:",     "[INVALID_DATE]");
        SHORT_TAGS.put("INVALID_EMAIL:",    "[INVALID_EMAIL]");
        SHORT_TAGS.put("INVALID_ID:",       "[INVALID_ID]");
        SHORT_TAGS.put("INVALID_NAME:",     "[INVALID_NAME]");
        SHORT_TAGS.put("INVALID_NUMBER:",   "[INVALID_NUMBER]");
        SHORT_TAGS.put("INVALID_PHONE:",    "[INVALID_PHONE]");
        SHORT_TAGS.put("INVALID_STATE:",    "[INVALID_STATE]");
        SHORT_TAGS.put("INVALID_TIME:",     "[INVALID_TIME]");
        SHORT_TAGS.put("INVALID_URL:",      "[INVALID_URL]");
        SHORT_TAGS.put("INVALID_ZIP:",      "[INVALID_ZIP]");
        SHORT_TAGS.put("NULL_VALUE:",       "[NULL_VALUE]");
        SHORT_TAGS.put("REJECTED:",         "[REJECTED]");
        SHORT_TAGS.put("SUSPICIOUS_DATE:",  "[SUSPICIOUS_DATE]");
    }
}