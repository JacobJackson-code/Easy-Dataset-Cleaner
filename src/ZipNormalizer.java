import java.util.regex.*;

public class ZipNormalizer {

    // ── Pre-compiled patterns ─────────────────────────────────────────────────
    private static final Pattern US_ZIP5      = Pattern.compile("^\\d{5}$");
    private static final Pattern US_ZIP9      = Pattern.compile("^(\\d{5})[\\-\\s](\\d{4})$");
    private static final Pattern US_ZIP9_BARE = Pattern.compile("^(\\d{5})(\\d{4})$");
    private static final Pattern CA_POSTAL    = Pattern.compile(
            "^([A-Za-z]\\d[A-Za-z])[\\s\\-]?(\\d[A-Za-z]\\d)$");
    private static final Pattern UK_POSTAL    = Pattern.compile(
            "^([A-Za-z]{1,2}\\d[A-Za-z\\d]?)[\\s]?(\\d[A-Za-z]{2})$");
    private static final Pattern DIGITS_ONLY  = Pattern.compile("^\\d+$");

    // ─────────────────────────────────────────────────────────────────────────
    // NORMALIZE
    //
    // formats:
    //   "zip5"       → 12345              (US 5-digit)
    //   "zip9"       → 12345-6789         (US ZIP+4 with dash)
    //   "zip9_space" → 12345 6789         (US ZIP+4 with space)
    //   "auto"       → detects type and normalizes to standard form
    //                  US → zip5 / zip9, CA → A1A 1A1, UK → AB1 1AB
    //
    // country: "US" / "CA" / "UK" / null (auto-detect)
    // ─────────────────────────────────────────────────────────────────────────
    public String normalize(String input, String format, String country) {
        if (input == null || input.trim().isEmpty()) return input;

        String trimmed = input.trim().toUpperCase();
        String fmt = format != null ? format.toLowerCase() : "auto";

        // ── Auto-detect country if not specified ──────────────────────────────
        String detectedCountry = country != null ? country.toUpperCase() : detectCountry(trimmed);

        switch (detectedCountry) {
            case "CA": return normalizeCA(trimmed, input);
            case "UK":
            case "GB": return normalizeUK(trimmed, input);
            default:   return normalizeUS(trimmed, fmt, input);
        }
    }

    // ── US ZIP ────────────────────────────────────────────────────────────────
    private String normalizeUS(String value, String fmt, String original) {
        // Clean: remove all non-digit and non-dash characters
        String cleaned = value.replaceAll("[^0-9\\-]", "");

        // Try ZIP5
        if (US_ZIP5.matcher(cleaned).matches()) {
            if (fmt.equals("zip9") || fmt.equals("zip9_space"))
                return "INVALID_ZIP: " + original + " (no +4 code available)";
            return cleaned; // just the 5 digits
        }

        // Try ZIP+4 with dash
        Matcher m9 = US_ZIP9.matcher(cleaned);
        if (m9.matches()) {
            String z5 = m9.group(1), z4 = m9.group(2);
            return formatZip9(z5, z4, fmt);
        }

        // Try ZIP+4 bare (no separator)
        Matcher m9b = US_ZIP9_BARE.matcher(cleaned.replaceAll("-", ""));
        if (m9b.matches()) {
            String z5 = m9b.group(1), z4 = m9b.group(2);
            if (fmt.equals("zip5")) return z5;
            return formatZip9(z5, z4, fmt);
        }

        // All digits but wrong length
        if (DIGITS_ONLY.matcher(cleaned).matches()) {
            if (cleaned.length() < 5) return "INVALID_ZIP: " + original + " (too short)";
            if (cleaned.length() > 9) return "INVALID_ZIP: " + original + " (too long)";
        }

        return "INVALID_ZIP: " + original;
    }

    private String formatZip9(String z5, String z4, String fmt) {
        if (fmt.equals("zip5")) return z5;
        if (fmt.equals("zip9_space")) return z5 + " " + z4;
        return z5 + "-" + z4; // default zip9 with dash
    }

    // ── Canadian postal code ──────────────────────────────────────────────────
    // Standard format: A1A 1A1
    private String normalizeCA(String value, String original) {
        Matcher m = CA_POSTAL.matcher(value);
        if (m.matches()) return m.group(1).toUpperCase() + " " + m.group(2).toUpperCase();
        return "INVALID_ZIP: " + original + " (invalid Canadian postal code)";
    }

    // ── UK postcode ───────────────────────────────────────────────────────────
    // Standard format: AB1 1AB or AB12 1AB etc.
    private String normalizeUK(String value, String original) {
        Matcher m = UK_POSTAL.matcher(value);
        if (m.matches()) return m.group(1).toUpperCase() + " " + m.group(2).toUpperCase();
        return "INVALID_ZIP: " + original + " (invalid UK postcode)";
    }

    // ── Auto-detect country from format ──────────────────────────────────────
    private String detectCountry(String value) {
        if (CA_POSTAL.matcher(value).matches()) return "CA";
        if (UK_POSTAL.matcher(value).matches()) return "UK";
        return "US"; // default
    }
}