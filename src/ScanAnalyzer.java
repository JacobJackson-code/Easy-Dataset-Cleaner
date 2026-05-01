import java.util.*;

public class ScanAnalyzer {

    // ─────────────────────────────────────────────────────────────────────────
    // SCAN ALL CONFIGURED COLUMNS
    // scanColumns: colName → scan type ("time" / "number" / "unique")
    // Returns report rows ready for writing to a file.
    // ─────────────────────────────────────────────────────────────────────────
    public List<List<String>> scan(List<List<String>> data,
                                   Map<String, String> scanColumns) {
        List<List<String>> report = new ArrayList<>();
        if (data == null || data.isEmpty() || scanColumns == null || scanColumns.isEmpty())
            return report;

        List<String> header = data.get(0);
        List<List<String>> rows = data.subList(1, data.size());

        report.add(row2("SCAN REPORT", ""));
        report.add(row2("Total Rows Scanned", String.valueOf(rows.size())));
        report.add(e());

        for (Map.Entry<String, String> entry : scanColumns.entrySet()) {
            String colName = entry.getKey();
            String scanType = entry.getValue().toLowerCase().trim();
            int colIdx = indexOf(header, colName);

            if (colIdx < 0) {
                report.add(row2("WARNING", "Column '" + colName + "' not found in header"));
                report.add(e());
                continue;
            }

            // Collect non-empty values for this column
            List<String> values = new ArrayList<>();
            for (List<String> row : rows) {
                if (colIdx < row.size() && row.get(colIdx) != null
                        && !row.get(colIdx).trim().isEmpty())
                    values.add(row.get(colIdx).trim());
            }

            report.add(row2("COLUMN: " + colName, "Scan Type: " + scanType.toUpperCase()));
            report.add(row2("Non-empty values", String.valueOf(values.size())));
            report.add(row2("Empty / missing", String.valueOf(rows.size() - values.size())));
            report.add(e());

            switch (scanType) {
                case "time":   report.addAll(scanTime(values));   break;
                case "number": report.addAll(scanNumber(values)); break;
                case "unique": report.addAll(scanUnique(values)); break;
                default:
                    report.add(row2("Unknown scan type", scanType
                            + " — use: time / number / unique"));
            }

            report.add(e());
            report.add(row2("─────────────────", ""));
            report.add(e());
        }

        return report;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // TIME SCAN
    // Detects: 12hr / 24hr / mixed / hour-only / timezone present / no timezone
    // ─────────────────────────────────────────────────────────────────────────
    private List<List<String>> scanTime(List<String> values) {
        List<List<String>> out = new ArrayList<>();
        if (values.isEmpty()) { out.add(row2("Result", "No values to scan")); return out; }

        int has12hr=0, has24hr=0, hasSeconds=0, hasTimezone=0,
                hourOnly=0, ambiguous=0, unparseable=0;

        List<String> examples12=new ArrayList<>(), examples24=new ArrayList<>(),
                examplesUnparseable=new ArrayList<>();

        for (String val : values) {
            String lower = val.toLowerCase().trim();
            boolean is12 = lower.contains("am") || lower.contains("pm");
            boolean hasTZ = lower.matches(".*[+\\-]\\d{2}:?\\d{2}.*")
                    || lower.matches(".*(utc|gmt|est|pst|cst|mst|edt|pdt|cdt|mdt"
                    + "|bst|cet|ist|jst|aest).*");
            boolean hasSec = val.matches(".*\\d{2}:\\d{2}:\\d{2}.*");
            boolean isHourOnly = val.matches("(?i)\\d{1,2}\\s*(am|pm)?")
                    && !val.contains(":");

            if (hasTZ)  hasTimezone++;
            if (hasSec) hasSeconds++;
            if (isHourOnly) hourOnly++;

            if (is12) {
                has12hr++;
                if (examples12.size() < 3) examples12.add(val);
            } else {
                // Check for 24hr: hour > 12
                try {
                    String hourStr = val.split(":")[0].trim();
                    int hour = Integer.parseInt(hourStr);
                    if (hour > 12) {
                        has24hr++;
                        if (examples24.size() < 3) examples24.add(val);
                    } else {
                        ambiguous++;
                    }
                } catch (Exception e) {
                    unparseable++;
                    if (examplesUnparseable.size() < 3) examplesUnparseable.add(val);
                }
            }
        }

        // Determine overall format
        String formatDetected;
        if (has12hr > 0 && has24hr > 0)
            formatDetected = "MIXED (12-hour and 24-hour both present)";
        else if (has12hr > 0)
            formatDetected = "12-hour (AM/PM)";
        else if (has24hr > 0)
            formatDetected = "24-hour";
        else
            formatDetected = "Ambiguous (values 1-12 could be either)";

        out.add(row2("Format Detected",    formatDetected));
        out.add(row2("12-hour values",     has12hr + exampleStr(examples12)));
        out.add(row2("24-hour values",     has24hr + exampleStr(examples24)));
        out.add(row2("Ambiguous values",   String.valueOf(ambiguous)));
        out.add(row2("Hour-only values",   String.valueOf(hourOnly)));
        out.add(row2("Has seconds",        hasSeconds > 0 ? "Yes (" + hasSeconds + " values)" : "No"));
        out.add(row2("Has timezone info",  hasTimezone > 0 ? "Yes (" + hasTimezone + " values)" : "No"));
        out.add(row2("Unparseable",        unparseable + exampleStr(examplesUnparseable)));
        out.add(e());

        // Recommendation
        String rec;
        if (has12hr > 0 && has24hr > 0)
            rec = "Mixed format detected. Recommend reviewing before normalizing. Use secondary order or column mapping to resolve.";
        else if (has12hr > 0)
            rec = "Suggest output format: \"hh:mm" + (hasSeconds > 0 ? ":ss" : "") + " a\"";
        else if (has24hr > 0)
            rec = "Suggest output format: \"HH:mm" + (hasSeconds > 0 ? ":ss" : "") + "\"";
        else
            rec = "Cannot determine format — review sample values manually.";

        if (hasTimezone > 0)
            rec += " | Timezone info present — set timezoneHandling to desired zone.";
        else
            rec += " | No timezone info detected — use \"strip\" or set globalTimezone if known.";

        out.add(row2("Recommendation", rec));
        return out;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // NUMBER SCAN
    // Detects: integer only / has decimals / max decimal places / negatives /
    //          parens notation / scientific notation / non-numeric entries
    // ─────────────────────────────────────────────────────────────────────────
    private List<List<String>> scanNumber(List<String> values) {
        List<List<String>> out = new ArrayList<>();
        if (values.isEmpty()) { out.add(row2("Result", "No values to scan")); return out; }

        int integerOnly=0, hasDecimals=0, hasNegative=0, hasParens=0,
                hasScientific=0, nonNumeric=0;
        int maxDecimalPlaces=0;
        List<String> examplesNonNumeric=new ArrayList<>(),
                examplesParens=new ArrayList<>(), examplesScientific=new ArrayList<>();

        for (String val : values) {
            String v = val.trim();

            if (v.matches(".*[eE][+\\-]?\\d+.*")) {
                hasScientific++;
                if (examplesScientific.size() < 3) examplesScientific.add(v);
                continue;
            }
            if (v.startsWith("(") && v.endsWith(")")) {
                hasParens++;
                if (examplesParens.size() < 3) examplesParens.add(v);
                v = "-" + v.substring(1, v.length()-1);
            }
            // Strip currency symbols and commas for parsing
            String cleaned = v.replaceAll("[^0-9.\\-]","");
            if (cleaned.isEmpty()) { nonNumeric++; if (examplesNonNumeric.size()<3) examplesNonNumeric.add(val); continue; }

            try {
                double d = Double.parseDouble(cleaned);
                if (d < 0) hasNegative++;

                if (cleaned.contains(".")) {
                    hasDecimals++;
                    int decPlaces = cleaned.length() - cleaned.indexOf('.') - 1;
                    if (decPlaces > maxDecimalPlaces) maxDecimalPlaces = decPlaces;
                } else {
                    integerOnly++;
                }
            } catch (NumberFormatException e) {
                nonNumeric++;
                if (examplesNonNumeric.size() < 3) examplesNonNumeric.add(val);
            }
        }

        String formatDetected;
        if (hasDecimals == 0 && nonNumeric == 0)
            formatDetected = "Integer only";
        else if (hasDecimals > 0 && integerOnly > 0)
            formatDetected = "Mixed (some integers, some decimals)";
        else if (hasDecimals > 0)
            formatDetected = "Decimal";
        else
            formatDetected = "Mostly numeric with non-numeric entries";

        out.add(row2("Format Detected",      formatDetected));
        out.add(row2("Integer values",        String.valueOf(integerOnly)));
        out.add(row2("Decimal values",        String.valueOf(hasDecimals)));
        out.add(row2("Max decimal places",    maxDecimalPlaces > 0 ? String.valueOf(maxDecimalPlaces) : "N/A"));
        out.add(row2("Negative values",       hasNegative > 0 ? "Yes (" + hasNegative + ")" : "No"));
        out.add(row2("Parens notation",       hasParens + exampleStr(examplesParens)));
        out.add(row2("Scientific notation",   hasScientific + exampleStr(examplesScientific)));
        out.add(row2("Non-numeric entries",   nonNumeric + exampleStr(examplesNonNumeric)));
        out.add(e());

        // Recommendation
        String rec;
        if (hasDecimals == 0 && nonNumeric == 0 && hasParens == 0 && hasScientific == 0)
            rec = "Suggest rule: \"integer\"";
        else if (hasDecimals > 0)
            rec = "Suggest rule: \"decimal:" + maxDecimalPlaces + "\"";
        else
            rec = "Review non-numeric entries before applying a rule.";

        if (hasParens > 0)
            rec += " | Parens notation found — add :standard to convert to negative.";
        if (hasScientific > 0)
            rec += " | Scientific notation found — set allowScientificNotation = true if intentional.";
        if (hasNegative > 0)
            rec += " | Negative values present.";

        out.add(row2("Recommendation", rec));
        return out;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // UNIQUE VALUE SCAN
    // Lists all unique values with their counts, sorted by frequency descending.
    // Perfect for setting up columnMappings and consistencyColumns.
    // ─────────────────────────────────────────────────────────────────────────
    private List<List<String>> scanUnique(List<String> values) {
        List<List<String>> out = new ArrayList<>();
        if (values.isEmpty()) { out.add(row2("Result", "No values to scan")); return out; }

        // Count occurrences
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (String val : values) {
            String key = val.trim();
            counts.put(key, counts.getOrDefault(key, 0) + 1);
        }

        // Sort by frequency descending
        List<Map.Entry<String, Integer>> sorted = new ArrayList<>(counts.entrySet());
        sorted.sort((a, b) -> b.getValue().compareTo(a.getValue()));

        out.add(row2("Unique values found", String.valueOf(sorted.size())));
        out.add(e());

        // Header for value table
        List<String> tableHeader = new ArrayList<>();
        tableHeader.add("Value");
        tableHeader.add("Count");
        tableHeader.add("% of Non-empty");
        out.add(tableHeader);

        for (Map.Entry<String, Integer> entry : sorted) {
            double pct = (double) entry.getValue() / values.size() * 100;
            List<String> row = new ArrayList<>();
            row.add(entry.getKey());
            row.add(String.valueOf(entry.getValue()));
            row.add(String.format("%.1f%%", pct));
            out.add(row);
        }

        out.add(e());
        if (sorted.size() <= 20)
            out.add(row2("Recommendation",
                    "Low cardinality — good candidate for consistencyColumns or columnMappings."));
        else if (sorted.size() <= 100)
            out.add(row2("Recommendation",
                    "Medium cardinality — review top entries for normalization opportunities."));
        else
            out.add(row2("Recommendation",
                    "High cardinality (" + sorted.size() + " unique values) — may not be a category column."));

        return out;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // HELPERS
    // ─────────────────────────────────────────────────────────────────────────

    private String exampleStr(List<String> examples) {
        if (examples.isEmpty()) return "";
        return "  (e.g. " + String.join(", ", examples) + ")";
    }

    private int indexOf(List<String> header, String colName) {
        for (int i = 0; i < header.size(); i++)
            if (header.get(i).equalsIgnoreCase(colName)) return i;
        return -1;
    }

    private List<String> row2(String a, String b) {
        List<String> r = new ArrayList<>(); r.add(a); r.add(b); return r;
    }

    private List<String> e() { return new ArrayList<>(); }
}