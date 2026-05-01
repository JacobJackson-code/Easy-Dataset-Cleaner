import java.util.*;

public class StateNormalizer {

    // ── Output format options ─────────────────────────────────────────────────
    // "abbr"     → CA
    // "full"     → California
    // "abbr_dot" → C.A.

    // Maps any recognized input → standard 2-letter abbreviation
    private static final Map<String, String> TO_ABBR = new LinkedHashMap<>();

    // Maps abbreviation → full name
    private static final Map<String, String> TO_FULL = new LinkedHashMap<>();

    static {
        String[][] states = {
                // abbr, full name, common variants (comma separated)
                {"AL","Alabama","ala"},
                {"AK","Alaska","alas"},
                {"AZ","Arizona","ariz"},
                {"AR","Arkansas","ark"},
                {"CA","California","calif,cal,cali,golden state"},
                {"CO","Colorado","colo,col"},
                {"CT","Connecticut","conn"},
                {"DE","Delaware","del"},
                {"FL","Florida","fla,flo"},
                {"GA","Georgia","geo"},
                {"HI","Hawaii",""},
                {"ID","Idaho","ida"},
                {"IL","Illinois","ill,ills"},
                {"IN","Indiana","ind"},
                {"IA","Iowa",""},
                {"KS","Kansas","kans,kan"},
                {"KY","Kentucky","ken,kent"},
                {"LA","Louisiana","lou,la"},
                {"ME","Maine",""},
                {"MD","Maryland",""},
                {"MA","Massachusetts","mass"},
                {"MI","Michigan","mich"},
                {"MN","Minnesota","minn,minn"},
                {"MS","Mississippi","miss"},
                {"MO","Missouri",""},
                {"MT","Montana","mont"},
                {"NE","Nebraska","neb,nebr"},
                {"NV","Nevada","nev"},
                {"NH","New Hampshire","n.h."},
                {"NJ","New Jersey","n.j."},
                {"NM","New Mexico","n.m.,n.mex."},
                {"NY","New York","n.y."},
                {"NC","North Carolina","n.c."},
                {"ND","North Dakota","n.d.,n.dak."},
                {"OH","Ohio",""},
                {"OK","Oklahoma","okla"},
                {"OR","Oregon","ore,oreg"},
                {"PA","Pennsylvania","penn,penna"},
                {"RI","Rhode Island","r.i."},
                {"SC","South Carolina","s.c."},
                {"SD","South Dakota","s.d.,s.dak."},
                {"TN","Tennessee","tenn"},
                {"TX","Texas","tex"},
                {"UT","Utah",""},
                {"VT","Vermont","vt"},
                {"VA","Virginia","va"},
                {"WA","Washington","wash,wa"},
                {"WV","West Virginia","w.v.,w.va."},
                {"WI","Wisconsin","wis,wisc"},
                {"WY","Wyoming","wyo"},
                {"DC","District of Columbia","d.c.,washington dc,washington d.c."},
        };

        for (String[] s : states) {
            String abbr = s[0];
            String full = s[1];
            String variants = s[2];

            TO_FULL.put(abbr, full);

            // Map abbreviation → abbr
            TO_ABBR.put(abbr.toLowerCase(), abbr);

            // Map full name → abbr
            TO_ABBR.put(full.toLowerCase(), abbr);

            // Map variants → abbr
            if (!variants.isEmpty()) {
                for (String v : variants.split(",")) {
                    String vt = v.trim();
                    if (!vt.isEmpty()) TO_ABBR.put(vt, abbr);
                }
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // NORMALIZE
    // format: "abbr" / "full" / "abbr_dot"
    // ─────────────────────────────────────────────────────────────────────────
    public String normalize(String input, String format) {
        if (input == null || input.trim().isEmpty()) return input;

        String cleaned = input.trim()
                .replaceAll("\\s+", " ")
                .toLowerCase();

        String abbr = TO_ABBR.get(cleaned);

        if (abbr == null) {
            // Try stripping dots: "C.A." → "ca"
            String stripped = cleaned.replace(".", "").replace(" ", "");
            abbr = TO_ABBR.get(stripped);
        }

        if (abbr == null) {
            return "INVALID_STATE: " + input.trim();
        }

        switch (format.toLowerCase()) {
            case "full":
                return TO_FULL.getOrDefault(abbr, abbr);
            case "abbr_dot":
                return abbr.charAt(0) + "." + abbr.charAt(1) + ".";
            case "abbr":
            default:
                return abbr;
        }
    }
}