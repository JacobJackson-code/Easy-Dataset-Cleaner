import java.util.regex.*;

public class PhoneNormalizer {

    // ── Pre-compiled patterns ─────────────────────────────────────────────────
    private static final Pattern DIGITS_ONLY      = Pattern.compile("[^0-9+]");
    private static final Pattern NON_DIGIT        = Pattern.compile("[^0-9]");
    private static final Pattern EXTENSION        = Pattern.compile(
            "(?i)\\s*(x|ext\\.?|extension)\\s*\\d+$");
    private static final Pattern US_10_DIGIT      = Pattern.compile("^1?(\\d{10})$");
    private static final Pattern HAS_COUNTRY_CODE = Pattern.compile("^\\+\\d");

    // ─────────────────────────────────────────────────────────────────────────
    // NORMALIZE
    //
    // formats:
    //   "E164"           → +12125551234       (international standard)
    //   "national"       → (212) 555-1234     (US standard display)
    //   "national_dash"  → 212-555-1234
    //   "national_dots"  → 212.555.1234
    //   "digits"         → 2125551234         (raw digits, no formatting)
    //
    // defaultCountry: assumed country for numbers with no + prefix
    //   "US" = assumed US/Canada (+1)
    //   null = no assumption, non-E164 numbers without country code
    //          are formatted using digits only
    // ─────────────────────────────────────────────────────────────────────────
    public String normalize(String input, String format, String defaultCountry) {
        if (input == null || input.trim().isEmpty()) return input;

        String trimmed = input.trim();

        // Strip extension before processing — we don't preserve it
        String withoutExt = EXTENSION.matcher(trimmed).replaceAll("").trim();

        // Extract just digits and leading +
        boolean hasPlus = withoutExt.startsWith("+");
        String digits = NON_DIGIT.matcher(withoutExt).replaceAll("");

        if (digits.isEmpty()) return "INVALID_PHONE: " + trimmed;

        // ── Determine full digit string with country code ─────────────────────
        String fullDigits; // always includes country code digits

        if (hasPlus) {
            // Has explicit country code
            fullDigits = digits;
        } else if ("US".equalsIgnoreCase(defaultCountry)) {
            // Assume US/Canada (+1)
            if (digits.length() == 10) {
                fullDigits = "1" + digits;
            } else if (digits.length() == 11 && digits.startsWith("1")) {
                fullDigits = digits;
            } else if (digits.length() < 7) {
                return "INVALID_PHONE: " + trimmed + " (too short)";
            } else if (digits.length() > 11) {
                return "INVALID_PHONE: " + trimmed + " (too long for US number)";
            } else {
                fullDigits = digits; // non-standard, keep as-is
            }
        } else {
            // No default country — use digits as-is
            fullDigits = digits;
        }

        // ── Basic length validation ───────────────────────────────────────────
        if (fullDigits.length() < 7) return "INVALID_PHONE: " + trimmed + " (too short)";
        if (fullDigits.length() > 15) return "INVALID_PHONE: " + trimmed + " (too long)";

        // ── Format output ─────────────────────────────────────────────────────
        String fmt = format != null ? format.toLowerCase() : "national";

        switch (fmt) {
            case "e164": {
                return "+" + fullDigits;
            }
            case "digits": {
                // For US, strip leading 1
                if ("US".equalsIgnoreCase(defaultCountry) && fullDigits.length() == 11
                        && fullDigits.startsWith("1")) {
                    return fullDigits.substring(1);
                }
                return fullDigits;
            }
            case "national":
            case "national_dash":
            case "national_dots": {
                // For US numbers — extract area code + 7 digit number
                String nationalDigits;
                if ("US".equalsIgnoreCase(defaultCountry)) {
                    if (fullDigits.length() == 11 && fullDigits.startsWith("1")) {
                        nationalDigits = fullDigits.substring(1);
                    } else if (fullDigits.length() == 10) {
                        nationalDigits = fullDigits;
                    } else {
                        // Non-US or unusual — just format with separator
                        return formatGeneric(fullDigits, fmt);
                    }
                    if (nationalDigits.length() != 10)
                        return "INVALID_PHONE: " + trimmed + " (expected 10 digits for US)";

                    String area     = nationalDigits.substring(0, 3);
                    String exchange = nationalDigits.substring(3, 6);
                    String number   = nationalDigits.substring(6, 10);

                    // Validate: area code and exchange can't start with 0 or 1
                    if (area.charAt(0) == '0' || area.charAt(0) == '1')
                        return "INVALID_PHONE: " + trimmed + " (invalid US area code)";
                    if (exchange.charAt(0) == '0' || exchange.charAt(0) == '1')
                        return "INVALID_PHONE: " + trimmed + " (invalid US exchange)";

                    switch (fmt) {
                        case "national":      return "(" + area + ") " + exchange + "-" + number;
                        case "national_dash": return area + "-" + exchange + "-" + number;
                        case "national_dots": return area + "." + exchange + "." + number;
                    }
                } else {
                    return formatGeneric(fullDigits, fmt);
                }
            }
            default:
                return "INVALID_PHONE: " + trimmed + " (unknown format: " + format + ")";
        }

    }

    // Generic formatting for non-US numbers — groups into readable chunks
    private String formatGeneric(String digits, String fmt) {
        String sep = fmt.equals("national_dots") ? "." : "-";
        if (digits.length() <= 7) return digits;
        // Group last 4, then 3, then rest
        String last4 = digits.substring(digits.length() - 4);
        String mid3  = digits.substring(Math.max(0, digits.length() - 7),
                digits.length() - 4);
        String rest  = digits.substring(0, Math.max(0, digits.length() - 7));
        if (rest.isEmpty()) return mid3 + sep + last4;
        return rest + sep + mid3 + sep + last4;
    }
}