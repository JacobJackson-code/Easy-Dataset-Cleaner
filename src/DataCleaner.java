import java.util.*;

public class DataCleaner {

    private boolean allowScientificNotation = false;

    // ── Normalizer instances — created once, reused across all calls ──────────
    private final CountryNormalizer countryNormalizer = new CountryNormalizer();
    private final DateNormalizer    dateNormalizer    = new DateNormalizer();
    private final StateNormalizer   stateNormalizer   = new StateNormalizer();
    private final TimeNormalizer    timeNormalizer    = new TimeNormalizer();
    private final PhoneNormalizer   phoneNormalizer   = new PhoneNormalizer();
    private final ZipNormalizer     zipNormalizer     = new ZipNormalizer();

    // ── Report builder — created once, stateless, reused across all calls ─────
    private final ReportBuilder reportBuilder = new ReportBuilder();

    // ── Pre-compiled hot regex patterns ───────────────────────────────────────
    private static final java.util.regex.Pattern PAT_DOUBLE_SPACE =
            java.util.regex.Pattern.compile(" {2,}");
    private static final java.util.regex.Pattern PAT_NEWLINES =
            java.util.regex.Pattern.compile("[\r\n]+");
    private static final java.util.regex.Pattern PAT_WHITESPACE =
            java.util.regex.Pattern.compile("\\s+");
    private NumberNormalizer numberNormalizer = new NumberNormalizer();

    // ── Silent change tracking ────────────────────────────────────────────────
    // Records value changes that happened without flagging.
    // Format per entry: [rowNumber, columnName, transformationType, originalValue, newValue]
    private final List<List<String>> silentChanges = new ArrayList<>();

    public void clearSilentChanges() { silentChanges.clear(); }
    public List<List<String>> getSilentChanges() { return new ArrayList<>(silentChanges); }

    private void recordChange(int rowNum, String colName, String transformation,
                              String original, String newValue) {
        if (original != null && newValue != null
                && !original.trim().isEmpty()
                && !original.equals(newValue)
                && !isDirty(newValue)) {
            List<String> change = new ArrayList<>();
            change.add(String.valueOf(rowNum));
            change.add(colName);
            change.add(transformation);
            change.add(original);
            change.add("→ " + newValue);
            silentChanges.add(change);
        }
    }

    // Converts a rule string to a plain-English description for silent-change records
    private String describeRule(String rule) {
        if (rule == null) return "Transformed";
        String r = rule.toLowerCase();
        if (r.startsWith("name:full"))               return "Name Formatted (Full)";
        if (r.startsWith("name:first"))              return "Name → First Only";
        if (r.startsWith("name:last"))               return "Name → Last Only";
        if (r.startsWith("name:initial_last"))       return "Name → Initial. Last";
        if (r.startsWith("name:initialnodot_last"))  return "Name → Initial.Last";
        if (r.startsWith("case:upper"))              return "Text → Uppercase";
        if (r.startsWith("case:lower"))              return "Text → Lowercase";
        if (r.startsWith("case:title"))              return "Text → Title Case";
        if (r.startsWith("case:sentence"))           return "Text → Sentence Case";
        if (r.startsWith("currency_code"))           return "Currency Code Standardized";
        if (r.startsWith("validate:email"))          return "Email Validated";
        if (r.startsWith("validate:url"))            return "URL Validated";
        if (r.startsWith("validate:any"))            return "Contact Validated";
        if (r.startsWith("id:"))                     return "ID Validated";
        if (r.startsWith("bool:"))                   return "Boolean Standardized";
        if (r.startsWith("currency:"))               return "Currency Formatted";
        if (r.startsWith("decimal:"))                return "Decimal Formatted";
        if (r.startsWith("integer"))                 return "Converted to Integer";
        if (r.startsWith("round:up"))                return "Rounded Up";
        if (r.startsWith("round:down"))              return "Rounded Down";
        if (r.startsWith("round:nearest"))           return "Rounded to Nearest";
        if (r.startsWith("words"))                   return "Converted to Words";
        return "Transformed (" + rule + ")";
    }

    public void setAllowScientificNotation(boolean allow) {
        this.allowScientificNotation = allow;
        this.numberNormalizer = new NumberNormalizer();
        this.numberNormalizer.setAllowScientificNotation(allow);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // isDirty  — must live here because every pipeline method calls it
    //
    // A cell is "dirty" if it is null, blank, or carries any flag prefix.
    // Flag prefix list lives in FlagConstants — one place for the whole system.
    // ─────────────────────────────────────────────────────────────────────────
    public boolean isDirty(String cell) {
        if (cell == null || cell.trim().isEmpty()) return true;
        String t = cell.trim();
        for (String prefix : FlagConstants.FLAG_PREFIXES)
            if (t.startsWith(prefix)) return true;
        return false;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // REPORTING DELEGATION
    //
    // Main.java calls these methods exactly as before.
    // All implementation lives in ReportBuilder — these are one-line wrappers.
    // When adding new report methods in the future: add to ReportBuilder first,
    // then add the delegation wrapper here.
    // ─────────────────────────────────────────────────────────────────────────

    public List<List<String>> extractFlagReport(
            List<List<String>> data,
            Set<String> identifierColumns,
            Map<String, List<Integer>> flagRowsByType,
            boolean showFlagsInOutput,
            Set<String> hiddenFlagTypes,
            Map<String, String> cellColorMap,
            String nullOutputFormat) {
        return reportBuilder.extractFlagReport(data, identifierColumns, flagRowsByType,
                showFlagsInOutput, hiddenFlagTypes, cellColorMap, nullOutputFormat,
                silentChanges);
    }

    public List<List<String>> extractFlagReport(List<List<String>> data,
                                                Set<String> identifierColumns,
                                                Map<String, List<Integer>> flagRowsByType) {
        return reportBuilder.extractFlagReport(data, identifierColumns, flagRowsByType);
    }

    public List<List<String>> extractFlagReport(List<List<String>> data,
                                                Set<String> identifierColumns) {
        return reportBuilder.extractFlagReport(data, identifierColumns);
    }

    public List<List<String>> extractFlagReport(List<List<String>> data) {
        return reportBuilder.extractFlagReport(data);
    }

    public List<List<String>> buildSummary(
            int originalRowCount,
            int finalRowCount,
            int rejectedRowCount,
            int multilineFixCount,
            Map<String, List<Integer>> flagRowsByType,
            String flagReportFileName) {
        return reportBuilder.buildSummary(originalRowCount, finalRowCount,
                rejectedRowCount, multilineFixCount, flagRowsByType, flagReportFileName);
    }

    public List<List<String>> buildConsistencyReport(
            List<Map<String, Object>> consistencyIssues,
            boolean normalizationEnabled) {
        return reportBuilder.buildConsistencyReport(consistencyIssues, normalizationEnabled);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CLEAN
    // ─────────────────────────────────────────────────────────────────────────
    public List<List<String>> clean(List<List<String>> data) {
        if (data == null || data.isEmpty()) return data;
        List<String> rawHeader = data.get(0);
        List<String> header = new ArrayList<>();
        for (String cell : rawHeader) header.add(cell != null ? cell.trim() : "");
        List<List<String>> rows = new ArrayList<>(data.subList(1, data.size()));
        rows = trimWhitespace(rows);
        rows = removeEmptyRows(rows);
        rows = removeDuplicates(rows);
        rows = removeRepeatedHeaders(header, rows);
        rows = applyTitleCase(header, rows);
        List<List<String>> result = new ArrayList<>();
        result.add(header);
        result.addAll(rows);
        return result;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // REQUIRED COLUMNS
    // ─────────────────────────────────────────────────────────────────────────
    public List<List<String>> applyRequiredColumns(List<String> header,
                                                   List<List<String>> rows,
                                                   Set<String> requiredColumns,
                                                   List<List<String>> rejected) {
        if (requiredColumns == null || requiredColumns.isEmpty()) return rows;
        Map<String, Integer> colIndexes = new HashMap<>();
        for (int i = 0; i < header.size(); i++)
            if (requiredColumns.contains(header.get(i)))
                colIndexes.put(header.get(i), i);
        List<List<String>> clean = new ArrayList<>();
        for (List<String> row : rows) {
            String missingCol = null;
            for (Map.Entry<String, Integer> e : colIndexes.entrySet()) {
                int idx = e.getValue();
                if (idx >= row.size() || row.get(idx) == null
                        || row.get(idx).trim().isEmpty()) {
                    missingCol = e.getKey(); break;
                }
            }
            if (missingCol != null) {
                List<String> r = new ArrayList<>();
                r.add("REJECTED: missing required value in column " + missingCol);
                r.addAll(row); rejected.add(r);
            } else clean.add(row);
        }
        return clean;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // COLUMN CONSISTENCY CHECK + OPTIONAL NORMALIZATION
    // ─────────────────────────────────────────────────────────────────────────
    public List<Map<String, Object>> applyColumnConsistency(
            List<String> header,
            List<List<String>> rows,
            Map<String, Set<String>> consistencyColumns,
            Map<String, Map<String, String>> consistencyNormalizers,
            boolean applyNormalization) {

        List<Map<String, Object>> report = new ArrayList<>();
        if (consistencyColumns == null || consistencyColumns.isEmpty()) return report;

        for (Map.Entry<String, Set<String>> entry : consistencyColumns.entrySet()) {
            String colName       = entry.getKey();
            Set<String> accepted = entry.getValue();
            int colIdx = indexOf(header, colName);
            if (colIdx < 0) {
                System.out.println("WARNING: consistency column '" + colName + "' not found");
                continue;
            }

            Map<String, String> acceptedNorm = new LinkedHashMap<>();
            for (String a : accepted) acceptedNorm.put(a.toLowerCase().trim(), a);

            Map<String, String> normMap = new LinkedHashMap<>();
            if (consistencyNormalizers != null && consistencyNormalizers.containsKey(colName))
                for (Map.Entry<String, String> ne : consistencyNormalizers.get(colName).entrySet())
                    normMap.put(ne.getKey().toLowerCase().trim(), ne.getValue());

            Map<String, List<Integer>> unrecognized = new LinkedHashMap<>();

            for (int rowIdx = 0; rowIdx < rows.size(); rowIdx++) {
                List<String> row = rows.get(rowIdx);
                if (colIdx >= row.size()) continue;
                String cell = row.get(colIdx);
                if (cell == null || cell.trim().isEmpty()) continue;
                if (isDirty(cell)) continue;

                String lower = cell.trim().toLowerCase();
                if (acceptedNorm.containsKey(lower)) {
                    if (applyNormalization) row.set(colIdx, acceptedNorm.get(lower));
                    continue;
                }
                if (normMap.containsKey(lower)) {
                    if (applyNormalization) row.set(colIdx, normMap.get(lower));
                    else unrecognized.computeIfAbsent(cell.trim(), k -> new ArrayList<>())
                            .add(rowIdx + 1);
                    continue;
                }
                unrecognized.computeIfAbsent(cell.trim(), k -> new ArrayList<>())
                        .add(rowIdx + 1);
            }

            if (!unrecognized.isEmpty()) {
                Map<String, Object> colReport = new LinkedHashMap<>();
                colReport.put("column",       colName);
                colReport.put("accepted",     String.join(", ", accepted));
                colReport.put("unrecognized", unrecognized);
                Set<Integer> allRows = new LinkedHashSet<>();
                for (List<Integer> rList : unrecognized.values()) allRows.addAll(rList);
                colReport.put("affectedRows", new ArrayList<>(allRows));
                report.add(colReport);
            }
        }
        return report;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // DOUBLE SPACE FIX
    // ─────────────────────────────────────────────────────────────────────────
    public List<List<String>> applyDoubleSpaceFix(List<List<String>> rows,
                                                  Set<String> allowDoubleSpaces,
                                                  List<String> header) {
        Set<Integer> allowed = new HashSet<>();
        for (int i = 0; i < header.size(); i++)
            if (allowDoubleSpaces.contains(header.get(i))) allowed.add(i);
        List<List<String>> result = new ArrayList<>();
        for (List<String> row : rows) {
            List<String> newRow = new ArrayList<>();
            for (int i = 0; i < row.size(); i++) {
                String cell = row.get(i);
                if (cell != null && !allowed.contains(i))
                    cell = PAT_DOUBLE_SPACE.matcher(cell).replaceAll(" ");
                newRow.add(cell);
            }
            result.add(newRow);
        }
        return result;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // MULTILINE FIX
    // ─────────────────────────────────────────────────────────────────────────
    // multilineFixCount tracks how many cells had newlines removed this run
    private int multilineFixCount = 0;
    public int getMultilineFixCount() { return multilineFixCount; }

    public List<List<String>> applyMultilineFix(List<List<String>> rows,
                                                Set<String> allowMultiline,
                                                List<String> header) {
        Set<Integer> allowed = new HashSet<>();
        for (int i = 0; i < header.size(); i++)
            if (allowMultiline.contains(header.get(i))) allowed.add(i);
        multilineFixCount = 0;
        List<List<String>> result = new ArrayList<>();
        for (List<String> row : rows) {
            List<String> newRow = new ArrayList<>();
            for (int i = 0; i < row.size(); i++) {
                String cell = row.get(i);
                if (cell != null && !allowed.contains(i)) {
                    boolean hadNewline = cell.contains("\n") || cell.contains("\r");
                    cell = PAT_NEWLINES.matcher(cell).replaceAll(" ");
                    cell = PAT_DOUBLE_SPACE.matcher(cell).replaceAll(" ").trim();
                    if (hadNewline) multilineFixCount++;
                }
                newRow.add(cell);
            }
            result.add(newRow);
        }
        return result;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // NULL FLAG DETECTION
    // ─────────────────────────────────────────────────────────────────────────
    public List<List<String>> applyNullFlagDetection(List<List<String>> rows,
                                                     Set<String> nullFlags) {
        if (nullFlags == null || nullFlags.isEmpty()) return rows;
        Set<String> norm = new HashSet<>();
        for (String f : nullFlags) if (f != null) norm.add(f.toLowerCase().trim());
        List<List<String>> result = new ArrayList<>();
        for (List<String> row : rows) {
            List<String> newRow = new ArrayList<>();
            for (String cell : row) {
                if (cell != null && norm.contains(cell.toLowerCase().trim()))
                    newRow.add("NULL_VALUE: " + cell);
                else newRow.add(cell);
            }
            result.add(newRow);
        }
        return result;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // ROW QUALITY CHECK
    // ─────────────────────────────────────────────────────────────────────────
    public List<List<String>> applyRowQualityCheck(List<List<String>> rows,
                                                   double minQuality,
                                                   List<List<String>> rejected) {
        List<List<String>> clean = new ArrayList<>();
        for (List<String> row : rows) {
            int total = row.size(); if (total == 0) continue;
            int cc = 0; for (String cell : row) if (!isDirty(cell)) cc++;
            double quality = (double) cc / total;
            if (quality >= minQuality) { clean.add(row); continue; }
            int qp = (int) Math.round(quality * 100), mp = (int) Math.round(minQuality * 100);
            List<String> r = new ArrayList<>();
            r.add("REJECTED: row quality too low (" + cc + " of " + total + " cells clean, "
                    + qp + "% below minimum " + mp + "%)");
            r.addAll(row); rejected.add(r);
        }
        return clean;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // STRUCTURAL VALIDATION
    // ─────────────────────────────────────────────────────────────────────────
    public List<List<String>> applyStructuralValidation(List<String> header,
                                                        List<List<String>> rows,
                                                        List<List<String>> rejected,
                                                        boolean allowMismatch) {
        int expected = header.size();
        List<List<String>> clean = new ArrayList<>();
        for (List<String> row : rows) {
            if (row.size() != expected) {
                if (allowMismatch) {
                    clean.add(row);
                } else {
                    List<String> r = new ArrayList<>();
                    r.add("REJECTED: wrong field count (" + row.size()
                            + " of " + expected + " expected)");
                    r.addAll(row); rejected.add(r);
                }
            } else clean.add(row);
        }
        return clean;
    }

    // Overload for backward compatibility
    public List<List<String>> applyStructuralValidation(List<String> header,
                                                        List<List<String>> rows,
                                                        List<List<String>> rejected) {
        return applyStructuralValidation(header, rows, rejected, false);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // DUPLICATE DETECTION
    // ─────────────────────────────────────────────────────────────────────────
    public List<List<String>> applyDuplicateDetection(List<String> header,
                                                      List<List<String>> rows,
                                                      Set<String> duplicateColumns,
                                                      List<List<String>> rejected) {
        if (duplicateColumns == null || duplicateColumns.isEmpty()) return rows;
        Map<String, Integer> colIndexes = new HashMap<>();
        for (int i = 0; i < header.size(); i++)
            if (duplicateColumns.contains(header.get(i)))
                colIndexes.put(header.get(i), i);

        Map<String, Integer> seenIndexes = new HashMap<>();
        List<List<String>> clean = new ArrayList<>();

        for (List<String> row : rows) {
            String dk = null, dc = null, dv = null;
            for (Map.Entry<String, Integer> e : colIndexes.entrySet()) {
                int idx = e.getValue(); if (idx >= row.size()) continue;
                String val = row.get(idx).trim(); if (val.isEmpty()) continue;
                String mk = e.getKey() + ":" + val.toLowerCase();
                if (seenIndexes.containsKey(mk)) { dk = mk; dc = e.getKey(); dv = val; break; }
            }
            if (dk == null) {
                for (Map.Entry<String, Integer> e : colIndexes.entrySet()) {
                    int idx = e.getValue(); if (idx >= row.size()) continue;
                    String val = row.get(idx).trim();
                    if (!val.isEmpty())
                        seenIndexes.put(e.getKey() + ":" + val.toLowerCase(), clean.size());
                }
                clean.add(row);
            } else {
                int ei = seenIndexes.get(dk);
                List<String> existing = clean.get(ei);
                int es = scoreRow(existing), ns = scoreRow(row);
                List<String> winner, loser;
                if (ns > es) {
                    winner = row; loser = existing; clean.set(ei, winner);
                    for (Map.Entry<String, Integer> e : colIndexes.entrySet()) {
                        int idx = e.getValue(); if (idx >= winner.size()) continue;
                        String val = winner.get(idx).trim();
                        if (!val.isEmpty())
                            seenIndexes.put(e.getKey() + ":" + val.toLowerCase(), ei);
                    }
                } else { winner = existing; loser = row; }
                List<String> r = new ArrayList<>();
                r.add("REJECTED: duplicate " + dc + " - " + dv
                        + " (kept higher quality row, score "
                        + Math.max(es, ns) + " vs " + Math.min(es, ns) + ")");
                r.addAll(loser); rejected.add(r);
            }
        }
        return clean;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // COLUMN RULES
    // ─────────────────────────────────────────────────────────────────────────
    public List<List<String>> applyColumnRules(List<String> header,
                                               List<List<String>> rows,
                                               Map<String, String> columnRules) {
        if (columnRules == null || columnRules.isEmpty()) return rows;
        Map<Integer, String> indexedRules = new HashMap<>();
        for (int i = 0; i < header.size(); i++)
            if (columnRules.containsKey(header.get(i)))
                indexedRules.put(i, columnRules.get(header.get(i)));
        List<List<String>> result = new ArrayList<>();
        for (List<String> row : rows) {
            List<String> newRow = new ArrayList<>(row);
            if (row.equals(header)) { result.add(row); continue; }
            for (Map.Entry<Integer, String> e : indexedRules.entrySet()) {
                int col = e.getKey(); if (col >= newRow.size()) continue;
                String before = newRow.get(col);
                String after  = numberNormalizer.apply(before, e.getValue(), null);
                newRow.set(col, after);
                String colName = col < header.size() ? header.get(col) : "Col" + col;
                recordChange(result.size() + 1, colName, describeRule(e.getValue()), before, after);
            }
            result.add(newRow);
        }
        return result;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // COLUMN CAPS
    // ─────────────────────────────────────────────────────────────────────────
    public List<List<String>> applyColumnCaps(List<String> header,
                                              List<List<String>> rows,
                                              Map<String, Long> columnCaps,
                                              Map<String, String> columnRules) {
        if (columnCaps == null || columnCaps.isEmpty()) return rows;
        Map<Integer, Long>   indexedCaps  = new HashMap<>();
        Map<Integer, String> indexedRules = new HashMap<>();
        for (int i = 0; i < header.size(); i++) {
            String col = header.get(i);
            if (columnCaps.containsKey(col)) {
                indexedCaps.put(i, columnCaps.get(col));
                indexedRules.put(i, columnRules.getOrDefault(col, "decimal"));
            }
        }
        List<List<String>> result = new ArrayList<>();
        for (List<String> row : rows) {
            List<String> newRow = new ArrayList<>(row);
            for (Map.Entry<Integer, Long> e : indexedCaps.entrySet()) {
                int col = e.getKey(); if (col >= newRow.size()) continue;
                newRow.set(col, numberNormalizer.apply(newRow.get(col),
                        indexedRules.getOrDefault(col, "decimal"), e.getValue()));
            }
            result.add(newRow);
        }
        return result;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // COLUMN MAPPINGS
    // ─────────────────────────────────────────────────────────────────────────
    public List<List<String>> applyColumnMappings(List<String> header,
                                                  List<List<String>> rows,
                                                  Map<String, Map<String, String>> columnMappings) {
        if (columnMappings == null || columnMappings.isEmpty()) return rows;
        Map<Integer, Map<String, String>> indexedMappings = new HashMap<>();
        for (int i = 0; i < header.size(); i++) {
            if (columnMappings.containsKey(header.get(i))) {
                Map<String, String> raw  = columnMappings.get(header.get(i));
                Map<String, String> norm = new HashMap<>();
                for (Map.Entry<String, String> e : raw.entrySet())
                    norm.put(e.getKey().toLowerCase().trim(), e.getValue());
                indexedMappings.put(i, norm);
            }
        }
        List<List<String>> result = new ArrayList<>();
        for (List<String> row : rows) {
            List<String> newRow = new ArrayList<>(row);
            for (Map.Entry<Integer, Map<String, String>> e : indexedMappings.entrySet()) {
                int col = e.getKey(); if (col >= newRow.size()) continue;
                String cell = newRow.get(col);
                if (cell == null || cell.trim().isEmpty()) continue;
                String key = cell.toLowerCase().trim();
                if (!isDirty(cell) && e.getValue().containsKey(key)) {
                    String mapped  = e.getValue().get(key);
                    String colName = col < header.size() ? header.get(col) : "Col" + col;
                    recordChange(result.size() + 1, colName, "Category Mapped", cell, mapped);
                    newRow.set(col, mapped);
                }
            }
            result.add(newRow);
        }
        return result;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // COUNTRY NORMALIZATION
    // ─────────────────────────────────────────────────────────────────────────
    public List<List<String>> applyCountryNormalization(List<String> header,
                                                        List<List<String>> rows,
                                                        Map<String, String> countryColumns) {
        if (countryColumns == null || countryColumns.isEmpty()) return rows;
        Map<Integer, String> indexedRules = new HashMap<>();
        for (int i = 0; i < header.size(); i++)
            if (countryColumns.containsKey(header.get(i)))
                indexedRules.put(i, countryColumns.get(header.get(i)));
        List<List<String>> result = new ArrayList<>();
        for (List<String> row : rows) {
            List<String> newRow = new ArrayList<>(row);
            for (Map.Entry<Integer, String> e : indexedRules.entrySet()) {
                int col = e.getKey(); if (col >= newRow.size()) continue;
                String before = newRow.get(col);
                if (isDirty(before)) continue;
                String after  = countryNormalizer.normalize(before, e.getValue());
                newRow.set(col, after);
                String colName = col < header.size() ? header.get(col) : "Col" + col;
                recordChange(result.size() + 1, colName, "Country Standardized", before, after);
            }
            result.add(newRow);
        }
        return result;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // STATE NORMALIZATION
    // ─────────────────────────────────────────────────────────────────────────
    public List<List<String>> applyStateNormalization(List<String> header,
                                                      List<List<String>> rows,
                                                      Map<String, String> stateColumns) {
        if (stateColumns == null || stateColumns.isEmpty()) return rows;
        Map<Integer, String> indexedRules = new HashMap<>();
        for (int i = 0; i < header.size(); i++)
            if (stateColumns.containsKey(header.get(i)))
                indexedRules.put(i, stateColumns.get(header.get(i)));
        List<List<String>> result = new ArrayList<>();
        for (List<String> row : rows) {
            List<String> newRow = new ArrayList<>(row);
            for (Map.Entry<Integer, String> e : indexedRules.entrySet()) {
                int col = e.getKey(); if (col >= newRow.size()) continue;
                String before = newRow.get(col);
                if (isDirty(before)) continue;
                String after  = stateNormalizer.normalize(before, e.getValue());
                newRow.set(col, after);
                String colName = col < header.size() ? header.get(col) : "Col" + col;
                recordChange(result.size() + 1, colName, "State Standardized", before, after);
            }
            result.add(newRow);
        }
        return result;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // DATE NORMALIZATION
    // ─────────────────────────────────────────────────────────────────────────
    public List<List<String>> applyDateNormalization(List<String> header,
                                                     List<List<String>> rows,
                                                     Map<String, String[]> dateColumns,
                                                     int suspiciousYearBefore,
                                                     int minYear, int maxYear) {
        if (dateColumns == null || dateColumns.isEmpty()) return rows;
        Map<Integer, String[]> indexedRules = new HashMap<>();
        for (int i = 0; i < header.size(); i++)
            if (dateColumns.containsKey(header.get(i)))
                indexedRules.put(i, dateColumns.get(header.get(i)));
        List<List<String>> result = new ArrayList<>();
        for (List<String> row : rows) {
            List<String> newRow = new ArrayList<>(row);
            for (Map.Entry<Integer, String[]> e : indexedRules.entrySet()) {
                int col = e.getKey(); if (col >= newRow.size()) continue;
                String before = newRow.get(col);
                if (isDirty(before)) continue;
                String[] rule         = e.getValue();
                String inputOrder     = rule.length > 0 ? rule[0] : "MDY";
                String outputFormat   = rule.length > 1 ? rule[1] : "MM/dd/yyyy";
                String secondaryOrder = rule.length > 2 ? rule[2] : null;
                String after = dateNormalizer.normalize(before, inputOrder, outputFormat,
                        suspiciousYearBefore, minYear, maxYear, secondaryOrder);
                newRow.set(col, after);
                String colName = col < header.size() ? header.get(col) : "Col" + col;
                recordChange(result.size() + 1, colName, "Date Reformatted", before, after);
            }
            result.add(newRow);
        }
        return result;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // TIME NORMALIZATION
    // ─────────────────────────────────────────────────────────────────────────
    public List<List<String>> applyTimeNormalization(List<String> header,
                                                     List<List<String>> rows,
                                                     Map<String, String[]> timeColumns,
                                                     String globalTimezone) {
        if (timeColumns == null || timeColumns.isEmpty()) return rows;
        Map<Integer, String[]> indexedRules = new HashMap<>();
        for (int i = 0; i < header.size(); i++)
            if (timeColumns.containsKey(header.get(i)))
                indexedRules.put(i, timeColumns.get(header.get(i)));
        List<List<String>> result = new ArrayList<>();
        for (List<String> row : rows) {
            List<String> newRow = new ArrayList<>(row);
            for (Map.Entry<Integer, String[]> e : indexedRules.entrySet()) {
                int col = e.getKey(); if (col >= newRow.size()) continue;
                String[] rule           = e.getValue();
                String outputFormat     = rule.length > 0 ? rule[0] : "HH:mm";
                String timezoneHandling = rule.length > 1 ? rule[1] : "strip";
                String before = newRow.get(col);
                if (isDirty(before)) continue;
                String after = timeNormalizer.normalize(before, outputFormat,
                        timezoneHandling, globalTimezone);
                newRow.set(col, after);
                String colName = col < header.size() ? header.get(col) : "Col" + col;
                recordChange(result.size() + 1, colName, "Time Reformatted", before, after);
            }
            result.add(newRow);
        }
        return result;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PHONE NORMALIZATION
    // phoneColumns: colName → {format, defaultCountry}
    //   format:        "E164" / "national" / "national_dash" / "national_dots" / "digits"
    //   defaultCountry: "US" or null
    // ─────────────────────────────────────────────────────────────────────────
    public List<List<String>> applyPhoneNormalization(List<String> header,
                                                      List<List<String>> rows,
                                                      Map<String, String[]> phoneColumns) {
        if (phoneColumns == null || phoneColumns.isEmpty()) return rows;
        Map<Integer, String[]> indexedRules = new HashMap<>();
        for (int i = 0; i < header.size(); i++)
            if (phoneColumns.containsKey(header.get(i)))
                indexedRules.put(i, phoneColumns.get(header.get(i)));
        List<List<String>> result = new ArrayList<>();
        for (List<String> row : rows) {
            List<String> newRow = new ArrayList<>(row);
            for (Map.Entry<Integer, String[]> e : indexedRules.entrySet()) {
                int col = e.getKey(); if (col >= newRow.size()) continue;
                String before = newRow.get(col);
                if (isDirty(before)) continue;
                String[] rule  = e.getValue();
                String format  = rule.length > 0 ? rule[0] : "national";
                String country = rule.length > 1 ? rule[1] : "US";
                String after   = phoneNormalizer.normalize(before, format, country);
                newRow.set(col, after);
                String colName = col < header.size() ? header.get(col) : "Col" + col;
                recordChange(result.size() + 1, colName, "Phone Formatted", before, after);
            }
            result.add(newRow);
        }
        return result;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // ZIP / POSTAL CODE NORMALIZATION
    // zipColumns: colName → {format, country}
    //   format:  "zip5" / "zip9" / "zip9_space" / "auto"
    //   country: "US" / "CA" / "UK" / null (auto-detect)
    // ─────────────────────────────────────────────────────────────────────────
    public List<List<String>> applyZipNormalization(List<String> header,
                                                    List<List<String>> rows,
                                                    Map<String, String[]> zipColumns) {
        if (zipColumns == null || zipColumns.isEmpty()) return rows;
        Map<Integer, String[]> indexedRules = new HashMap<>();
        for (int i = 0; i < header.size(); i++)
            if (zipColumns.containsKey(header.get(i)))
                indexedRules.put(i, zipColumns.get(header.get(i)));
        List<List<String>> result = new ArrayList<>();
        for (List<String> row : rows) {
            List<String> newRow = new ArrayList<>(row);
            for (Map.Entry<Integer, String[]> e : indexedRules.entrySet()) {
                int col = e.getKey(); if (col >= newRow.size()) continue;
                String before = newRow.get(col);
                if (isDirty(before)) continue;
                String[] rule  = e.getValue();
                String format  = rule.length > 0 ? rule[0] : "auto";
                String country = rule.length > 1 ? rule[1] : null;
                String after   = zipNormalizer.normalize(before, format, country);
                newRow.set(col, after);
                String colName = col < header.size() ? header.get(col) : "Col" + col;
                recordChange(result.size() + 1, colName, "ZIP Formatted", before, after);
            }
            result.add(newRow);
        }
        return result;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CURRENCY-AWARE DECIMAL FORMATTING
    // ─────────────────────────────────────────────────────────────────────────
    public List<List<String>> applyCurrencyAwareDecimals(List<String> header,
                                                         List<List<String>> rows,
                                                         Map<String, String> currencyAwareColumns,
                                                         String mode) {
        if (currencyAwareColumns == null || currencyAwareColumns.isEmpty()) return rows;
        Map<Integer, Integer> indexMap = new HashMap<>();
        for (Map.Entry<String, String> e : currencyAwareColumns.entrySet()) {
            int ai = indexOf(header, e.getKey()), ci = indexOf(header, e.getValue());
            if (ai >= 0 && ci >= 0) indexMap.put(ai, ci);
            else {
                if (ai < 0) System.out.println("WARNING: currency-aware column '"
                        + e.getKey() + "' not found");
                if (ci < 0) System.out.println("WARNING: currency code column '"
                        + e.getValue() + "' not found");
            }
        }
        if (indexMap.isEmpty()) return rows;
        List<List<String>> result = new ArrayList<>();
        for (List<String> row : rows) {
            List<String> newRow = new ArrayList<>(row);
            for (Map.Entry<Integer, Integer> e : indexMap.entrySet()) {
                int ai = e.getKey(), ci = e.getValue();
                if (ai >= newRow.size() || ci >= newRow.size()) continue;
                String amount   = newRow.get(ai);
                String currency = newRow.get(ci);
                if (amount == null || amount.trim().isEmpty()) continue;
                if (currency == null || currency.trim().isEmpty()) continue;
                if (isDirty(currency) || isDirty(amount)) continue;
                newRow.set(ai, numberNormalizer.formatForCurrency(
                        amount, currency.trim().toUpperCase(), mode));
            }
            result.add(newRow);
        }
        return result;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // SORT ROWS
    // sortColumn:  column name to sort by
    // sortOrder:   "asc" or "desc"
    // sortExtract: null = parse full value as number
    //              "last_number"  = extract last numeric segment  USR-0042 → 42
    //              "numeric_only" = strip all non-digits          USR-0042 → 42
    //              "position:N"   = take Nth segment after split  USR-2024-0042, position:3 → 0042
    // ─────────────────────────────────────────────────────────────────────────
    public List<List<String>> applySortRows(List<String> header,
                                            List<List<String>> rows,
                                            String sortColumn,
                                            String sortOrder,
                                            String sortExtract) {
        if (sortColumn == null || sortColumn.isEmpty()) return rows;
        int colIdx = indexOf(header, sortColumn);
        if (colIdx < 0) {
            System.out.println("WARNING: sort column '" + sortColumn + "' not found");
            return rows;
        }

        boolean desc = "desc".equalsIgnoreCase(sortOrder);
        List<List<String>> sorted = new ArrayList<>(rows);
        sorted.sort((a, b) -> {
            String va = colIdx < a.size() ? a.get(colIdx) : "";
            String vb = colIdx < b.size() ? b.get(colIdx) : "";
            Double da = extractSortKey(va, sortExtract);
            Double db = extractSortKey(vb, sortExtract);
            if (da == null && db == null) return va.compareTo(vb);
            if (da == null) return desc ? -1 : 1;
            if (db == null) return desc ? 1 : -1;
            int cmp = da.compareTo(db);
            return desc ? -cmp : cmp;
        });
        return sorted;
    }

    private Double extractSortKey(String value, String sortExtract) {
        if (value == null || value.trim().isEmpty()) return null;
        String v = value.trim();
        if (isDirty(v)) return null;  // sort flagged cells last
        try {
            if (sortExtract == null)
                return Double.parseDouble(v.replaceAll("[,$€£¥'\\s]", "").replace(",", ""));
            String lower = sortExtract.toLowerCase().trim();
            if (lower.equals("last_number")) {
                java.util.regex.Matcher m =
                        java.util.regex.Pattern.compile("\\d+").matcher(v);
                String last = null;
                while (m.find()) last = m.group();
                return last != null ? Double.parseDouble(last) : null;
            }
            if (lower.equals("numeric_only")) {
                String digits = v.replaceAll("[^0-9]", "");
                return digits.isEmpty() ? null : Double.parseDouble(digits);
            }
            if (lower.startsWith("position:")) {
                int pos = Integer.parseInt(lower.substring(9).trim()) - 1;
                String[] segments = v.split("[^a-zA-Z0-9]+");
                if (pos < 0 || pos >= segments.length) return null;
                return Double.parseDouble(segments[pos]);
            }
        } catch (Exception ex) {
            // Cannot extract numeric key — fall back to string sort
        }
        return null;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // FILTER ROWS
    // ─────────────────────────────────────────────────────────────────────────
    public List<List<String>> filterRows(List<List<String>> data, String keyword) {
        if (data == null || keyword == null || keyword.isEmpty()) return data;
        List<List<String>> filtered = new ArrayList<>();
        for (List<String> row : data)
            for (String cell : row)
                if (cell != null && cell.toLowerCase().contains(keyword.toLowerCase())) {
                    filtered.add(row); break;
                }
        return filtered;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PRIVATE HELPERS
    // ─────────────────────────────────────────────────────────────────────────

    private int scoreRow(List<String> row) {
        int score = 0;
        for (String cell : row) if (isDirty(cell)) score--;
        return score;
    }

    private int indexOf(List<String> header, String colName) {
        for (int i = 0; i < header.size(); i++)
            if (header.get(i).equalsIgnoreCase(colName)) return i;
        return -1;
    }

    private List<List<String>> trimWhitespace(List<List<String>> data) {
        List<List<String>> cleaned = new ArrayList<>();
        for (List<String> row : data) {
            List<String> newRow = new ArrayList<>();
            for (String cell : row) newRow.add(cell != null ? cell.trim() : "");
            cleaned.add(newRow);
        }
        return cleaned;
    }

    private List<List<String>> removeEmptyRows(List<List<String>> data) {
        List<List<String>> cleaned = new ArrayList<>();
        for (List<String> row : data) {
            boolean isEmpty = true;
            for (String cell : row)
                if (cell != null && !cell.trim().isEmpty()) { isEmpty = false; break; }
            if (!isEmpty) cleaned.add(row);
        }
        return cleaned;
    }

    private List<List<String>> removeDuplicates(List<List<String>> data) {
        List<List<String>> cleaned = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (List<String> row : data) {
            String key = row.toString();
            if (!seen.contains(key)) { seen.add(key); cleaned.add(row); }
        }
        return cleaned;
    }

    private List<List<String>> applyTitleCase(List<String> header, List<List<String>> rows) {
        Set<Integer> nameColumns = new HashSet<>();
        for (int i = 0; i < header.size(); i++)
            if (header.get(i).toLowerCase().contains("name")) nameColumns.add(i);
        List<List<String>> result = new ArrayList<>();
        for (List<String> row : rows) {
            List<String> newRow = new ArrayList<>();
            for (int i = 0; i < row.size(); i++) {
                String cell = row.get(i);
                if (nameColumns.contains(i) && cell != null && !cell.isEmpty()) {
                    String[] words = cell.toLowerCase().split(" ");
                    StringBuilder sb = new StringBuilder();
                    for (String word : words)
                        if (!word.isEmpty())
                            sb.append(Character.toUpperCase(word.charAt(0)))
                                    .append(word.substring(1)).append(" ");
                    newRow.add(sb.toString().trim());
                } else newRow.add(cell);
            }
            result.add(newRow);
        }
        return result;
    }

    private List<List<String>> removeRepeatedHeaders(List<String> header,
                                                     List<List<String>> rows) {
        List<String> norm = new ArrayList<>();
        for (String h : header) norm.add(h.toLowerCase().trim());
        List<List<String>> result = new ArrayList<>();
        for (List<String> row : rows) {
            if (row.size() != header.size()) { result.add(row); continue; }
            boolean isHeader = true;
            for (int i = 0; i < row.size(); i++) {
                String cell = row.get(i) != null ? row.get(i).toLowerCase().trim() : "";
                if (!cell.equals(norm.get(i))) { isHeader = false; break; }
            }
            if (!isHeader) result.add(row);
        }
        return result;
    }
}