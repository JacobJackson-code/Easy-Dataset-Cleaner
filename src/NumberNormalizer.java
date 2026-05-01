import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.*;

public class NumberNormalizer {

    // ── Delegation ────────────────────────────────────────────────────────────
    private final TextNormalizer textNormalizer = new TextNormalizer();

    // ── Scientific notation setting ───────────────────────────────────────────
    private boolean allowScientificNotation = false;

    public void setAllowScientificNotation(boolean allow) {
        this.allowScientificNotation = allow;
    }

    // ── Currency symbols ──────────────────────────────────────────────────────
    private static final Map<String, String> CURRENCY_SYMBOLS = new LinkedHashMap<>();
    private static final Set<String> KNOWN_CURRENCY_CODES = new HashSet<>();

    public enum FormatStyle { STANDARD, EUROPEAN, SWISS, INDIAN }

    private static final Map<String, FormatStyle> CURRENCY_STYLES   = new HashMap<>();
    private static final Map<String, Integer>     CURRENCY_DECIMALS = new HashMap<>();

    static {
        CURRENCY_SYMBOLS.put("USD","$");    CURRENCY_SYMBOLS.put("EUR","€");
        CURRENCY_SYMBOLS.put("GBP","£");    CURRENCY_SYMBOLS.put("JPY","¥");
        CURRENCY_SYMBOLS.put("INR","₹");    CURRENCY_SYMBOLS.put("CAD","CA$");
        CURRENCY_SYMBOLS.put("AUD","A$");   CURRENCY_SYMBOLS.put("CHF","CHF ");
        CURRENCY_SYMBOLS.put("MXN","MX$");  CURRENCY_SYMBOLS.put("BRL","R$");
        CURRENCY_SYMBOLS.put("CNY","¥");    CURRENCY_SYMBOLS.put("KRW","₩");
        CURRENCY_SYMBOLS.put("SGD","S$");   CURRENCY_SYMBOLS.put("HKD","HK$");
        CURRENCY_SYMBOLS.put("NOK","kr");   CURRENCY_SYMBOLS.put("SEK","kr");
        CURRENCY_SYMBOLS.put("DKK","kr");   CURRENCY_SYMBOLS.put("NZD","NZ$");
        CURRENCY_SYMBOLS.put("ZAR","R");    CURRENCY_SYMBOLS.put("KWD","KD");
        CURRENCY_SYMBOLS.put("BHD","BD");   CURRENCY_SYMBOLS.put("OMR","OMR");
        CURRENCY_SYMBOLS.put("JOD","JD");   CURRENCY_SYMBOLS.put("TND","DT");
        CURRENCY_SYMBOLS.put("IQD","IQD");  CURRENCY_SYMBOLS.put("LYD","LD");
        KNOWN_CURRENCY_CODES.addAll(CURRENCY_SYMBOLS.keySet());

        for (String c : Arrays.asList("EUR","NOK","SEK","DKK","ISK"))
            CURRENCY_STYLES.put(c, FormatStyle.EUROPEAN);
        CURRENCY_STYLES.put("CHF", FormatStyle.SWISS);
        CURRENCY_STYLES.put("INR", FormatStyle.INDIAN);

        for (String c : Arrays.asList(
                "JPY","KRW","VND","IDR","HUF","CLP","ISK","PYG",
                "UGX","RWF","GNF","KMF","MGA","DJF","BIF","XOF","XAF","XPF"))
            CURRENCY_DECIMALS.put(c, 0);
        for (String c : Arrays.asList("KWD","BHD","OMR","JOD","TND","IQD","LYD"))
            CURRENCY_DECIMALS.put(c, 3);
    }

    private static final Map<String, Long> WORD_MAP = new LinkedHashMap<>();
    static {
        WORD_MAP.put("zero",0L); WORD_MAP.put("one",1L); WORD_MAP.put("two",2L);
        WORD_MAP.put("three",3L); WORD_MAP.put("four",4L); WORD_MAP.put("five",5L);
        WORD_MAP.put("six",6L); WORD_MAP.put("seven",7L); WORD_MAP.put("eight",8L);
        WORD_MAP.put("nine",9L); WORD_MAP.put("ten",10L); WORD_MAP.put("eleven",11L);
        WORD_MAP.put("twelve",12L); WORD_MAP.put("thirteen",13L);
        WORD_MAP.put("fourteen",14L); WORD_MAP.put("fifteen",15L);
        WORD_MAP.put("sixteen",16L); WORD_MAP.put("seventeen",17L);
        WORD_MAP.put("eighteen",18L); WORD_MAP.put("nineteen",19L);
        WORD_MAP.put("twenty",20L); WORD_MAP.put("thirty",30L);
        WORD_MAP.put("forty",40L); WORD_MAP.put("fifty",50L);
        WORD_MAP.put("sixty",60L); WORD_MAP.put("seventy",70L);
        WORD_MAP.put("eighty",80L); WORD_MAP.put("ninety",90L);
        WORD_MAP.put("hundred",100L); WORD_MAP.put("thousand",1000L);
        WORD_MAP.put("million",1000000L);
    }

    private static final String NEG_STANDARD = "standard";
    private static final String NEG_PARENS   = "parens";
    private static final String NEG_FLAG     = "flag";

    // ─────────────────────────────────────────────────────────────────────────
    // PUBLIC ENTRY POINTS
    // ─────────────────────────────────────────────────────────────────────────

    public String apply(String input, String rule) { return apply(input, rule, null); }

    public String apply(String input, String rule, Long cap) {
        if (input == null || input.trim().isEmpty()) return input;
        if (rule == null || rule.trim().isEmpty()) return input;

        String t = input.trim();
        if (FlagConstants.isDirty(t)) return input;

        String trimmed = t.replaceAll("^\"+|\"+$", "");
        if (trimmed.isEmpty()) return input;
        if (trimmed.equals("∞") || trimmed.equals("-∞")
                || trimmed.equalsIgnoreCase("NaN")
                || trimmed.equalsIgnoreCase("infinity")
                || trimmed.equalsIgnoreCase("-infinity"))
            return "INVALID_NUMBER: " + input;
        input = trimmed;

        String[] parts = rule.trim().split(":");
        String type = parts[0].toLowerCase();

        String negStyle = NEG_FLAG;
        for (String p : parts) {
            String pl = p.toLowerCase();
            if (pl.equals(NEG_STANDARD)) { negStyle = NEG_STANDARD; break; }
            if (pl.equals(NEG_PARENS))   { negStyle = NEG_PARENS;   break; }
            if (pl.equals(NEG_FLAG))     { negStyle = NEG_FLAG;      break; }
        }

        boolean isParens = isParensNotation(input);
        if (isParens) {
            if (negStyle.equals(NEG_FLAG)) return "INVALID_NUMBER: " + input;
            input = parensToNegative(input);
        }

        String result;
        switch (type) {
            case "integer":
                result = toInteger(input);
                if (result == null) return "INVALID_NUMBER: " + input;
                break;
            case "words":
                result = toWords(input);
                break;
            case "currency":
                result = formatCurrency(input, toLower(parts));
                if (result == null) return "INVALID_NUMBER: " + input;
                break;
            case "decimal":
                result = formatDecimal(input, toLower(parts));
                if (result == null) return "INVALID_NUMBER: " + input;
                break;
            case "round":
                result = formatRound(input, toLower(parts));
                if (result == null) return "INVALID_NUMBER: " + input;
                break;
            case "percent":
                result = formatPercent(input, toLower(parts));
                if (result == null) return "INVALID_NUMBER: " + input;
                break;
            case "case":          return textNormalizer.applyCase(input, toLower(parts));
            case "currency_code": return applyCurrencyCode(input, toLower(parts));
            case "name":          return textNormalizer.applyName(input, toLower(parts));
            case "validate":      return textNormalizer.applyValidate(input, toLower(parts));
            case "id":            return textNormalizer.applyId(input, parts);
            case "bool":          return textNormalizer.applyBool(input, parts);
            default:              return input;
        }

        if (negStyle.equals(NEG_PARENS) && result != null)
            result = applyParensOutput(result);

        if (cap != null && result != null) {
            BigDecimal parsed = parseNumber(result);
            if (parsed != null && parsed.compareTo(BigDecimal.valueOf(cap)) > 0)
                return "CAP_EXCEEDED: " + result;
        }
        return result;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CURRENCY-AWARE FORMATTING
    // ─────────────────────────────────────────────────────────────────────────
    public String formatForCurrency(String input, String currencyCode, String mode) {
        if (input == null || input.trim().isEmpty()) return input;
        String t = input.trim();
        if (isDirty(t)) return input;
        BigDecimal value = parseNumber(input);
        if (value == null) return "INVALID_NUMBER: " + input;
        String code = currencyCode.toUpperCase().trim();
        int decimals = CURRENCY_DECIMALS.getOrDefault(code, 2);
        switch (mode.toLowerCase()) {
            case "decimal":
                return decimals > 0
                        ? String.format("%." + decimals + "f", value.setScale(decimals, RoundingMode.HALF_UP))
                        : String.valueOf(value.setScale(0, RoundingMode.HALF_UP).longValue());
            case "nogrouping":
                return formatByStyle(value, decimals, CURRENCY_STYLES.getOrDefault(code, FormatStyle.STANDARD), false);
            default:
                return formatByStyle(value, decimals, CURRENCY_STYLES.getOrDefault(code, FormatStyle.STANDARD), true);
        }
    }

    private boolean isDirty(String cell) {
        if (cell == null || cell.trim().isEmpty()) return true;
        String t = cell.trim();
        return t.startsWith("INVALID_") || t.startsWith("CAP_EXCEEDED:")
                || t.startsWith("NULL_VALUE:") || t.startsWith("REJECTED:")
                || t.startsWith("SUSPICIOUS_DATE:");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // FORMAT BY STYLE
    // ─────────────────────────────────────────────────────────────────────────
    private String formatByStyle(BigDecimal value, int decimals, FormatStyle style, boolean useThousands) {
        BigDecimal scaled = value.setScale(decimals, RoundingMode.HALF_UP);
        switch (style) {
            case EUROPEAN: {
                if (!useThousands)
                    return decimals > 0 ? String.format("%." + decimals + "f", scaled).replace(".", ",") : String.valueOf(scaled.longValue());
                DecimalFormatSymbols sym = new DecimalFormatSymbols();
                sym.setGroupingSeparator('.'); sym.setDecimalSeparator(',');
                return new DecimalFormat(buildPattern(decimals), sym).format(scaled);
            }
            case SWISS: {
                if (!useThousands)
                    return decimals > 0 ? String.format("%." + decimals + "f", scaled) : String.valueOf(scaled.longValue());
                DecimalFormatSymbols sym = new DecimalFormatSymbols();
                sym.setGroupingSeparator('\''); sym.setDecimalSeparator('.');
                return new DecimalFormat(buildPattern(decimals), sym).format(scaled);
            }
            case INDIAN: return formatIndian(scaled, decimals, useThousands);
            default: {
                if (!useThousands)
                    return decimals > 0 ? String.format("%." + decimals + "f", scaled) : String.valueOf(scaled.longValue());
                return new DecimalFormat(buildPattern(decimals), DecimalFormatSymbols.getInstance(Locale.US)).format(scaled);
            }
        }
    }

    private String buildPattern(int decimals) {
        StringBuilder p = new StringBuilder("#,##0");
        if (decimals > 0) { p.append("."); for (int i = 0; i < decimals; i++) p.append("0"); }
        return p.toString();
    }

    private String formatIndian(BigDecimal value, int decimals, boolean useThousands) {
        String plain = value.toPlainString();
        String intPart; String decPart = "";
        if (plain.contains(".")) { intPart = plain.substring(0, plain.indexOf('.')); decPart = plain.substring(plain.indexOf('.')); }
        else { intPart = plain; }
        if (decimals > 0) { while (decPart.length() - 1 < decimals) decPart += "0"; if (decPart.isEmpty()) decPart = "." + String.valueOf('0').repeat(decimals); }
        else { decPart = ""; }
        if (!useThousands || intPart.length() <= 3) return intPart + decPart;
        StringBuilder result = new StringBuilder();
        result.insert(0, intPart.substring(intPart.length() - 3));
        intPart = intPart.substring(0, intPart.length() - 3);
        while (intPart.length() > 2) { result.insert(0, intPart.substring(intPart.length() - 2) + ","); intPart = intPart.substring(0, intPart.length() - 2); }
        if (!intPart.isEmpty()) result.insert(0, intPart + ",");
        return result.toString() + decPart;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CURRENCY FORMAT
    // ─────────────────────────────────────────────────────────────────────────
    private String formatPercent(String input, String[] parts) {
        int places = 2;
        boolean useCommas = !contains(parts, "nocomma");
        for (int i = 1; i < parts.length; i++) {
            try { places = Integer.parseInt(parts[i]); break; }
            catch (NumberFormatException ignored) {}
        }
        String cleaned = input.trim().replace("%", "").trim();
        BigDecimal value = parseNumber(cleaned);
        if (value == null) return null;
        return formatWithDecimals(value, places, useCommas) + "%";
    }

    private String formatCurrency(String input, String[] parts) {
        String code = (parts.length > 1) ? parts[1].toUpperCase() : "USD";
        boolean useCommas = !contains(parts, "nocomma");
        BigDecimal value = parseNumber(input);
        if (value == null) return null;
        String symbol = CURRENCY_SYMBOLS.getOrDefault(code, code + " ");
        return symbol + formatWithDecimals(value, 2, useCommas);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // DECIMAL
    // ─────────────────────────────────────────────────────────────────────────
    private String formatDecimal(String input, String[] parts) {
        int places = 2; boolean useCommas = !contains(parts, "nocomma");
        for (int i = 1; i < parts.length; i++) { try { places = Integer.parseInt(parts[i]); break; } catch (NumberFormatException ignored) {} }
        BigDecimal value = parseNumber(input);
        if (value == null) return null;
        return formatWithDecimals(value, places, useCommas);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // ROUND
    // ─────────────────────────────────────────────────────────────────────────
    private String formatRound(String input, String[] parts) {
        String direction = (parts.length > 1) ? parts[1] : "nearest";
        boolean useCommas = !contains(parts, "nocomma");
        BigDecimal value = parseNumber(input);
        if (value == null) return null;
        BigDecimal rounded;
        switch (direction) { case "up": rounded = value.setScale(0, RoundingMode.CEILING); break; case "down": rounded = value.setScale(0, RoundingMode.FLOOR); break; default: rounded = value.setScale(0, RoundingMode.HALF_UP); }
        return formatWholeNumber(rounded.longValue(), useCommas);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // INTEGER
    // ─────────────────────────────────────────────────────────────────────────
    public String toInteger(String input) {
        if (input == null || input.trim().isEmpty()) return input;
        BigDecimal parsed = parseNumber(input);
        if (parsed != null) return String.valueOf(parsed.longValue());
        try {
            String cleaned = input.trim().toLowerCase().replace("-", " ").replace(",", "");
            String[] tokens = cleaned.split("\\s+");
            long result = 0, current = 0; boolean foundAny = false;
            for (String token : tokens) {
                if (!WORD_MAP.containsKey(token)) continue;
                foundAny = true; long val = WORD_MAP.get(token);
                if (val == 100)                          { current = current == 0 ? 100 : current * 100; }
                else if (val == 1000 || val == 1000000)  { current = current == 0 ? val : current * val; result += current; current = 0; }
                else                                     { current += val; }
            }
            result += current;
            if (!foundAny) return null;
            if (result == 0 && !input.toLowerCase().contains("zero")) return null;
            return String.valueOf(result);
        } catch (Exception e) { return null; }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // WORDS
    // ─────────────────────────────────────────────────────────────────────────
    public String toWords(String input) {
        if (input == null || input.trim().isEmpty()) return input;
        BigDecimal parsed = parseNumber(input);
        if (parsed == null) return "INVALID_NUMBER: " + input;
        try { return convertToWords(parsed.longValue()); } catch (Exception e) { return "INVALID_NUMBER: " + input; }
    }

    private String convertToWords(long number) {
        if (number == 0) return "Zero";
        if (number < 0) return "Negative " + convertToWords(-number);
        String[] ones = {"","One","Two","Three","Four","Five","Six","Seven","Eight","Nine","Ten","Eleven","Twelve","Thirteen","Fourteen","Fifteen","Sixteen","Seventeen","Eighteen","Nineteen"};
        String[] tens = {"","","Twenty","Thirty","Forty","Fifty","Sixty","Seventy","Eighty","Ninety"};
        if (number < 20)    return ones[(int) number];
        if (number < 100)   { String t = tens[(int) number / 10]; String o = ones[(int) number % 10]; return o.isEmpty() ? t : t + "-" + o; }
        if (number < 1000)  { String r = convertToWords(number % 100); String h = ones[(int) number / 100] + " Hundred"; return r.isEmpty() ? h : h + " " + r; }
        if (number < 1000000) { String r = convertToWords(number % 1000); String t = convertToWords(number / 1000) + " Thousand"; return r.isEmpty() ? t : t + " " + r; }
        String r = convertToWords(number % 1000000); String m = convertToWords(number / 1000000) + " Million"; return r.isEmpty() ? m : m + " " + r;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // NEGATIVE PARENS
    // ─────────────────────────────────────────────────────────────────────────
    private boolean isParensNotation(String input) {
        if (input == null) return false;
        String t = input.trim();
        return t.startsWith("(") && t.endsWith(")");
    }

    private String parensToNegative(String input) {
        String inner = input.trim();
        inner = inner.substring(1, inner.length() - 1).trim();
        return "-" + inner;
    }

    private String applyParensOutput(String result) {
        if (result == null) return null;
        String t = result.trim();
        if (t.startsWith("(") && t.endsWith(")")) return result;
        if (t.startsWith("-")) return "(" + t.substring(1) + ")";
        return result;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CURRENCY CODE NORMALIZATION
    // Kept in NumberNormalizer: depends on CURRENCY_SYMBOLS and KNOWN_CURRENCY_CODES
    // which also drive parseNumber() — data and consumers stay together.
    // ─────────────────────────────────────────────────────────────────────────
    private String applyCurrencyCode(String input, String[] parts) {
        if (input == null || input.trim().isEmpty()) return input;
        if (parts.length < 2) return input;
        String upper = input.trim().toUpperCase();
        if (!KNOWN_CURRENCY_CODES.contains(upper)) return "INVALID_CURRENCY: " + input;
        switch (parts[1]) {
            case "upper":  return upper;
            case "lower":  return input.trim().toLowerCase();
            case "symbol": return CURRENCY_SYMBOLS.getOrDefault(upper, "INVALID_CURRENCY: " + input);
            default:       return input;
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // HELPERS
    // ─────────────────────────────────────────────────────────────────────────
    private BigDecimal parseNumber(String input) {
        if (input == null) return null;
        String cleaned = input.trim().replaceAll("^\"+|\"+$", "").replaceAll("(?i)^(INVALID_NUMBER:|CAP_EXCEEDED:)\\s*", "");
        if (cleaned.startsWith("(") && cleaned.endsWith(")"))
            cleaned = "-" + cleaned.substring(1, cleaned.length() - 1).trim();
        List<String> symbols = new ArrayList<>(CURRENCY_SYMBOLS.values());
        symbols.sort((a, b) -> b.length() - a.length());
        for (String sym : symbols) cleaned = cleaned.replace(sym.trim(), "");
        for (String code : CURRENCY_SYMBOLS.keySet()) cleaned = cleaned.replaceAll("(?i)\\b" + code + "\\b", "");
        cleaned = cleaned.trim();
        if (!allowScientificNotation && cleaned.matches(".*[eE][+\\-]?\\d+.*")) return null;
        boolean hasComma = cleaned.contains(","), hasPeriod = cleaned.contains(".");
        if (hasComma && hasPeriod) { if (cleaned.lastIndexOf(",") > cleaned.lastIndexOf(".")) cleaned = cleaned.replace(".", "").replace(",", "."); else cleaned = cleaned.replace(",", ""); }
        else if (hasComma) { String ac = cleaned.substring(cleaned.lastIndexOf(",") + 1); cleaned = ac.length() == 3 ? cleaned.replace(",", "") : cleaned.replace(",", "."); }
        cleaned = cleaned.replace("'", "").replaceAll("\\s+", "").trim();
        if (cleaned.isEmpty()) return null;
        try { return new BigDecimal(cleaned); } catch (NumberFormatException e) { return null; }
    }

    private String formatWithDecimals(BigDecimal value, int places, boolean useCommas) {
        BigDecimal scaled = value.setScale(places, RoundingMode.HALF_UP);
        if (useCommas) return new DecimalFormat(buildPattern(places), DecimalFormatSymbols.getInstance(Locale.US)).format(scaled);
        return places > 0 ? String.format("%." + places + "f", scaled) : String.valueOf(scaled.longValue());
    }

    private String formatWholeNumber(long value, boolean useCommas) {
        if (useCommas) return new DecimalFormat("#,##0", DecimalFormatSymbols.getInstance(Locale.US)).format(value);
        return String.valueOf(value);
    }

    private String[] toLower(String[] parts) {
        String[] lower = new String[parts.length];
        for (int i = 0; i < parts.length; i++) lower[i] = parts[i] != null ? parts[i].toLowerCase() : "";
        return lower;
    }

    private boolean contains(String[] parts, String token) {
        for (String p : parts) if (p.equalsIgnoreCase(token)) return true;
        return false;
    }
}