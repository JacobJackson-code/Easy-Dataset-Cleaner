import java.util.*;

public class TextNormalizer {

    // ── Name suffixes / prefixes ──────────────────────────────────────────────
    private static final Map<String, String> NAME_SUFFIXES = new LinkedHashMap<>();
    private static final Set<String> NAME_PREFIXES = new HashSet<>(Arrays.asList(
            "mr","mr.","mrs","mrs.","ms","ms.","miss","dr","dr.",
            "prof","prof.","rev","rev."));
    static {
        NAME_SUFFIXES.put("jr","Jr.");   NAME_SUFFIXES.put("jr.","Jr.");
        NAME_SUFFIXES.put("sr","Sr.");   NAME_SUFFIXES.put("sr.","Sr.");
        NAME_SUFFIXES.put("ii","II");    NAME_SUFFIXES.put("iii","III");
        NAME_SUFFIXES.put("iv","IV");    NAME_SUFFIXES.put("esq","Esq.");
        NAME_SUFFIXES.put("esq.","Esq."); NAME_SUFFIXES.put("md","MD");
        NAME_SUFFIXES.put("phd","PhD");  NAME_SUFFIXES.put("dds","DDS");
    }

    // ── Extended boolean map ──────────────────────────────────────────────────
    private static final Map<String, Boolean> BOOL_MAP = new HashMap<>();
    static {
        // Truthy values
        for (String s : Arrays.asList(
                "true","yes","1","on","enabled","active","y","t",
                "ok","positive","correct","valid",
                "confirm","confirmed","accept","accepted","approved",
                "agree","agreed","success","succeeded","successful",
                "open","pass","passed","complete","completed","done",
                "allow","allowed","available","checked","selected",
                "present","exists","found","match","matched","grant","granted"))
            BOOL_MAP.put(s, Boolean.TRUE);

        // Falsy values
        for (String s : Arrays.asList(
                "false","no","0","off","disabled","inactive","n","f",
                "negative","incorrect","invalid",
                "deny","denied","reject","rejected","declined","decline",
                "disagree","fail","failed","failure","unsuccessful",
                "closed","block","blocked","unavailable","unchecked",
                "absent","missing","not found","none","null",
                "error","revoke","revoked","cancel","cancelled","canceled",
                "pending","void","voided","expired"))
            BOOL_MAP.put(s, Boolean.FALSE);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CASE NORMALIZATION
    // ─────────────────────────────────────────────────────────────────────────
    String applyCase(String input, String[] parts) {
        if (parts.length < 2) return input;
        switch (parts[1]) {
            case "upper":    return input.toUpperCase();
            case "lower":    return input.toLowerCase();
            case "title":    return toTitleCase(input);
            case "sentence": return toSentenceCase(input);
            default:         return input;
        }
    }

    private String toTitleCase(String input) {
        if (input == null || input.isEmpty()) return input;
        String[] words = input.toLowerCase().split(" ");
        StringBuilder sb = new StringBuilder();
        for (String word : words) if (!word.isEmpty()) sb.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1)).append(" ");
        return sb.toString().trim();
    }

    private String toSentenceCase(String input) {
        if (input == null || input.isEmpty()) return input;
        String lower = input.toLowerCase().trim();
        return Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // NAME NORMALIZATION
    // Modes: full / first / last / initial_last / initialnodot_last
    // ─────────────────────────────────────────────────────────────────────────
    String applyName(String input, String[] parts) {
        String mode = (parts.length > 1) ? parts[1] : "full";
        String cleaned = input.trim()
                .replace(";", ",").replace("|", ",").replace("/", ",")
                .replaceAll("[^a-zA-Z\\s,.'\\-]", "").replaceAll("\\s+", " ").trim();
        if (cleaned.contains(",")) {
            String[] cp = cleaned.split(",", 2);
            if (cp.length == 2) cleaned = cp[1].trim() + " " + cp[0].trim();
        }
        // Split compact initials: C.Lee → C. Lee  c.lee → c. lee
        // Negative lookbehind ensures we only split a true initial, not mid-word
        cleaned = cleaned.replaceAll("(?i)(?<![a-zA-Z])([a-zA-Z])\\.([a-zA-Z]{2,})", "$1. $2");
        String[] tokens = cleaned.split("\\s+");
        List<String> nameTokens = new ArrayList<>();
        String suffix = null;
        for (String token : tokens) {
            String lower = token.toLowerCase().replaceAll("\\.$", "");
            if (NAME_PREFIXES.contains(lower) || NAME_PREFIXES.contains(lower + ".")) continue;
            if (NAME_SUFFIXES.containsKey(lower)) suffix = NAME_SUFFIXES.get(lower);
            else nameTokens.add(token);
        }
        if (nameTokens.isEmpty()) return "INVALID_NAME: " + input;

        List<String> cased = new ArrayList<>();
        for (String token : nameTokens) cased.add(capitalizeNameToken(token));

        StringBuilder full = new StringBuilder();
        for (String tok : cased) full.append(tok).append(" ");
        String fullName = full.toString().trim();
        if (suffix != null) fullName = fullName + " " + suffix;

        switch (mode) {
            case "first":
                return cased.isEmpty() ? fullName : cased.get(0);
            case "last":
                return cased.isEmpty() ? fullName : cased.get(cased.size() - 1);
            case "initial_last": {
                // J. Jackson
                if (cased.size() < 2) return "INVALID_NAME: " + input.trim() + " (need first and last)";
                String first = cased.get(0);
                String last = cased.get(cased.size() - 1);
                String init = String.valueOf(Character.toUpperCase(first.charAt(0))) + ".";
                return init + " " + last;
            }
            case "initialnodot_last": {
                // J.Jackson
                if (cased.size() < 2) return "INVALID_NAME: " + input.trim() + " (need first and last)";
                String first = cased.get(0);
                String last = cased.get(cased.size() - 1);
                String init = String.valueOf(Character.toUpperCase(first.charAt(0)));
                return init + "." + last;
            }
            case "full":
            default:
                if (cased.size() < 2) return "INVALID_NAME: " + input.trim() + " (single name only)";
                return fullName;
        }
    }

    private String capitalizeNameToken(String token) {
        if (token == null || token.isEmpty()) return token;
        if (token.contains("-")) {
            String[] h = token.split("-");
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < h.length; i++) { sb.append(capitalizeNameToken(h[i])); if (i < h.length - 1) sb.append("-"); }
            return sb.toString();
        }
        if (token.matches("(?i)o'.*") && token.length() > 2) return "O'" + capitalizeFirst(token.substring(2));
        if (token.matches("(?i)mc.+") && token.length() > 2) return "Mc" + capitalizeFirst(token.substring(2));
        if (token.matches("(?i)mac[^aeiou].+") && token.length() > 3) return "Mac" + capitalizeFirst(token.substring(3));
        return capitalizeFirst(token);
    }

    private String capitalizeFirst(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1).toLowerCase();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // BOOLEAN NORMALIZATION
    // ─────────────────────────────────────────────────────────────────────────
    String applyBool(String input, String[] parts) {
        if (input == null || input.trim().isEmpty()) return input;
        String trueOut = "true"; String falseOut = "false";
        if (parts.length > 1) {
            String[] outputs = parts[1].split("/");
            if (outputs.length == 2) { trueOut = outputs[0]; falseOut = outputs[1]; }
        }
        Boolean boolValue = BOOL_MAP.get(input.trim().toLowerCase());
        if (boolValue == null) return "INVALID_BOOL: " + input;
        return boolValue ? trueOut : falseOut;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // ID VALIDATION
    // ─────────────────────────────────────────────────────────────────────────
    String applyId(String input, String[] parts) {
        if (input == null || input.trim().isEmpty()) return input;
        String value = input.trim();
        String requiredStart = null, requiredEnd = null;
        for (int i = 1; i < parts.length; i++) {
            String p = parts[i].toLowerCase();
            if (p.equals("startswith") && i + 1 < parts.length) requiredStart = parts[++i];
            else if (p.equals("endswith") && i + 1 < parts.length) requiredEnd = parts[++i];
        }
        if (requiredStart != null) {
            String req = requiredStart.toLowerCase();
            if (req.equals("letter"))       { if (value.isEmpty() || !Character.isLetter(value.charAt(0)))           return "INVALID_ID: " + input + " (expected to start with a letter)"; }
            else if (req.equals("number"))  { if (value.isEmpty() || !Character.isDigit(value.charAt(0)))            return "INVALID_ID: " + input + " (expected to start with a number)"; }
            else                            { if (!value.toLowerCase().startsWith(requiredStart.toLowerCase()))       return "INVALID_ID: " + input + " (expected to start with '" + requiredStart + "')"; }
        }
        if (requiredEnd != null) {
            String req = requiredEnd.toLowerCase();
            if (req.equals("letter"))       { if (value.isEmpty() || !Character.isLetter(value.charAt(value.length() - 1))) return "INVALID_ID: " + input + " (expected to end with a letter)"; }
            else if (req.equals("number"))  { if (value.isEmpty() || !Character.isDigit(value.charAt(value.length() - 1)))  return "INVALID_ID: " + input + " (expected to end with a number)"; }
            else                            { if (!value.toLowerCase().endsWith(requiredEnd.toLowerCase()))                  return "INVALID_ID: " + input + " (expected to end with '" + requiredEnd + "')"; }
        }
        return value;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // VALIDATE: EMAIL / URL / ANY
    // ─────────────────────────────────────────────────────────────────────────
    String applyValidate(String input, String[] parts) {
        String mode = (parts.length > 1) ? parts[1] : "any";
        switch (mode) {
            case "email": return validateEmail(input);
            case "url":   return validateUrl(input);
            case "any":
                String er = validateEmail(input);
                if (!er.startsWith("INVALID_EMAIL:")) return er;
                String ur = validateUrl(input);
                if (!ur.startsWith("INVALID_URL:")) return ur;
                return "INVALID_CONTACT: " + input;
            default: return input;
        }
    }

    private String validateEmail(String input) {
        if (input == null || input.trim().isEmpty()) return input;
        String cleaned = input.trim().replaceAll("\\s*@\\s*", "@").replaceAll("\\s*\\.\\s*", ".").toLowerCase();
        long atCount = cleaned.chars().filter(c -> c == '@').count();
        if (atCount != 1) return "INVALID_EMAIL: " + input;
        int at = cleaned.indexOf('@');
        String local = cleaned.substring(0, at), domain = cleaned.substring(at + 1);
        if (local.isEmpty() || domain.isEmpty() || !domain.contains(".")) return "INVALID_EMAIL: " + input;
        int lastDot = domain.lastIndexOf('.');
        if (domain.substring(0, lastDot).isEmpty()) return "INVALID_EMAIL: " + input;
        String tld = domain.substring(lastDot + 1);
        if (tld.isEmpty() || !tld.matches("[a-z]+")) return "INVALID_EMAIL: " + input;
        return cleaned;
    }

    private String validateUrl(String input) {
        if (input == null || input.trim().isEmpty()) return input;
        String cleaned = input.trim().replaceAll("\\s+", "").toLowerCase();
        String scheme = "", rest = cleaned;
        if (cleaned.startsWith("https://"))      { scheme = "https://"; rest = cleaned.substring(8); }
        else if (cleaned.startsWith("http://"))  { scheme = "http://";  rest = cleaned.substring(7); }
        String host = rest.startsWith("www.") ? rest.substring(4) : rest;
        if (!host.contains(".") || host.indexOf('.') == 0) return "INVALID_URL: " + input;
        String tld = host.substring(host.lastIndexOf('.') + 1).split("[/?#]")[0];
        if (tld.isEmpty() || !tld.matches("[a-z0-9]+")) return "INVALID_URL: " + input;
        return scheme + (rest.startsWith("www.") ? "www." : "") + host;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // HELPERS (duplicated from NumberNormalizer — both are self-contained)
    // ─────────────────────────────────────────────────────────────────────────
    String[] toLower(String[] parts) {
        String[] lower = new String[parts.length];
        for (int i = 0; i < parts.length; i++) lower[i] = parts[i] != null ? parts[i].toLowerCase() : "";
        return lower;
    }

    boolean contains(String[] parts, String token) {
        for (String p : parts) if (p.equalsIgnoreCase(token)) return true;
        return false;
    }
}
