import java.util.*;

/**
 * Builds all output reports for the Easy-Dataset-Cleaner pipeline.
 *
 * RESPONSIBILITIES:
 *   extractFlagReport     — scans cleaned data for flags, populates color map,
 *                           shortens flag prefixes in clean output, appends
 *                           silent-conversion INFO section
 *   buildSummary          — high-level statistics row for the summary file
 *   buildConsistencyReport — formats column-consistency issues into rows
 *
 * DESIGN CONTRACT:
 *   - This class is STATELESS. Every method takes all inputs as parameters
 *     and returns a value. No fields are mutated between calls.
 *   - It does NOT call any DataCleaner pipeline methods.
 *   - It does NOT own silentChanges. DataCleaner owns that list and passes
 *     it in when calling extractFlagReport.
 *   - All flag definitions live in FlagConstants. Do not copy them here.
 *
 * Called by DataCleaner via delegation wrappers — Main.java is unaware
 * of this class and its API must never change in response to changes here.
 */
public class ReportBuilder {

    // Maximum number of row numbers to list inline before appending "..."
    // (affects summary and consistency report row-reference strings)
    private static final int MAX_SUMMARY_ROWS_LISTED = 10;

    // ─────────────────────────────────────────────────────────────────────────
    // EXTRACT FLAG REPORT  (full signature)
    //
    // Parameters:
    //   data             — full cleaned dataset including header row (row 0)
    //   identifierColumns — column names to include as identifier context
    //   flagRowsByType   — populated in place: flagType → list of row indexes
    //   showFlagsInOutput — false = restore original value, no tag in clean file
    //   hiddenFlagTypes  — specific flag types to hide from clean file
    //   cellColorMap     — populated in place: "rowIdx:colIdx" → flagType
    //   nullOutputFormat — replacement string for NULL_VALUE cells (null = leave tag)
    //   silentChanges    — list of silent-change records from DataCleaner;
    //                      appended as INFO section at bottom of report
    //
    // Side effects on data:
    //   Each flagged cell in data is rewritten in place:
    //     - If hidden or showFlagsInOutput=false → originalValue (tag removed)
    //     - If NULL_VALUE + nullOutputFormat set → nullOutputFormat string
    //     - Otherwise → "[SHORT_TAG] originalValue"
    //
    // Return value:
    //   Report rows ready to write to the flag-report output file.
    //   Sorted HIGH → MEDIUM → LOW. INFO section appended if silentChanges given.
    // ─────────────────────────────────────────────────────────────────────────
    public List<List<String>> extractFlagReport(
            List<List<String>> data,
            Set<String> identifierColumns,
            Map<String, List<Integer>> flagRowsByType,
            boolean showFlagsInOutput,
            Set<String> hiddenFlagTypes,
            Map<String, String> cellColorMap,
            String nullOutputFormat,
            List<List<String>> silentChanges) {

        List<List<String>> report = new ArrayList<>();
        if (data.isEmpty()) return report;

        List<String> header = data.get(0);

        // Resolve identifier column indexes once up front
        List<Integer> identifierIndexes = new ArrayList<>();
        List<String>  identifierNames   = new ArrayList<>();
        if (identifierColumns != null) {
            for (String col : identifierColumns) {
                int idx = indexOf(header, col);
                if (idx >= 0) {
                    identifierIndexes.add(idx);
                    identifierNames.add(header.get(idx));
                }
            }
        }

        // Build report header row
        List<String> detailHeader = new ArrayList<>();
        detailHeader.add("Severity");   // colored indicator column
        detailHeader.add("Row");
        detailHeader.addAll(identifierNames);
        detailHeader.add("Column");
        detailHeader.add("Flag Type");
        detailHeader.add("Original Value");
        detailHeader.add("Reason");
        report.add(detailHeader);

        // Severity buckets for ordered output: HIGH → MEDIUM → LOW
        List<List<String>> highRows   = new ArrayList<>();
        List<List<String>> mediumRows = new ArrayList<>();
        List<List<String>> lowRows    = new ArrayList<>();

        for (int rowIdx = 1; rowIdx < data.size(); rowIdx++) {
            List<String> row = data.get(rowIdx);

            for (int colIdx = 0; colIdx < row.size(); colIdx++) {
                String cell = row.get(colIdx);
                if (cell == null) continue;

                // Identify which flag prefix this cell carries (if any)
                String matchedPrefix = null;
                for (String prefix : FlagConstants.FLAG_PREFIXES) {
                    if (cell.trim().startsWith(prefix)) { matchedPrefix = prefix; break; }
                }
                if (matchedPrefix == null) continue;

                // Parse the value and optional reason out of the flag string
                // Format written by normalizers:  PREFIX originalValue (reason)
                String afterPrefix   = cell.trim().substring(matchedPrefix.length()).trim();
                String flagType      = matchedPrefix.replace(":", "");
                String originalValue = afterPrefix;
                String reason        = "";

                int parenIdx = afterPrefix.lastIndexOf("(");
                if (parenIdx > 0 && afterPrefix.endsWith(")")) {
                    originalValue = afterPrefix.substring(0, parenIdx).trim();
                    reason        = afterPrefix.substring(parenIdx);
                }

                // Accumulate row numbers per flag type for summary
                if (flagRowsByType != null) {
                    flagRowsByType.computeIfAbsent(flagType, k -> new ArrayList<>());
                    List<Integer> rowList = flagRowsByType.get(flagType);
                    if (rowList.isEmpty() || rowList.get(rowList.size() - 1) != rowIdx)
                        rowList.add(rowIdx);
                }

                // Record coordinate for cell coloring (must happen before content changes)
                if (cellColorMap != null) {
                    cellColorMap.put(rowIdx + ":" + colIdx, flagType);
                }

                String colName = colIdx < header.size() ? header.get(colIdx) : "Col" + colIdx;

                // Severity classification
                String severity;
                if      (flagType.startsWith("INVALID"))     severity = "HIGH";
                else if (flagType.startsWith("SUSPICIOUS"))  severity = "MEDIUM";
                else                                          severity = "LOW";

                // Build the flag-report row
                List<String> flagRow = new ArrayList<>();
                flagRow.add(severity);
                flagRow.add(String.valueOf(rowIdx));
                for (int idIdx : identifierIndexes)
                    flagRow.add((idIdx < row.size() && row.get(idIdx) != null)
                            ? row.get(idIdx) : "");
                flagRow.add(colName);
                flagRow.add(flagType);
                flagRow.add(originalValue);
                flagRow.add(reason);

                if      (severity.equals("HIGH"))   highRows.add(flagRow);
                else if (severity.equals("MEDIUM"))  mediumRows.add(flagRow);
                else                                 lowRows.add(flagRow);

                // Rewrite the cell in the clean output dataset
                boolean hideThisFlag = !showFlagsInOutput
                        || (hiddenFlagTypes != null && hiddenFlagTypes.contains(flagType));

                if (flagType.equals("NULL_VALUE") && nullOutputFormat != null) {
                    row.set(colIdx, nullOutputFormat);
                } else if (hideThisFlag) {
                    row.set(colIdx, originalValue);
                } else {
                    String shortTag = FlagConstants.SHORT_TAGS.getOrDefault(matchedPrefix, "[FLAG]");
                    row.set(colIdx, shortTag + " " + originalValue);
                }
            }
        }

        // Append rows in severity order
        report.addAll(highRows);
        report.addAll(mediumRows);
        report.addAll(lowRows);

        // Append silent-conversion INFO section (if any conversions were tracked)
        if (silentChanges != null && !silentChanges.isEmpty()) {
            report.add(new ArrayList<>());  // blank separator row

            List<String> silentHeader = new ArrayList<>();
            silentHeader.add("INFO");
            silentHeader.add("SILENT CONVERSIONS");
            report.add(silentHeader);

            List<String> silentColHeader = new ArrayList<>();
            silentColHeader.add("INFO");
            silentColHeader.add("Row");
            silentColHeader.add("Column");
            silentColHeader.add("Transformation");
            silentColHeader.add("Original Value");
            silentColHeader.add("New Value");
            report.add(silentColHeader);

            for (List<String> change : silentChanges) {
                List<String> infoRow = new ArrayList<>();
                infoRow.add("INFO");     // severity indicator for coloring
                infoRow.addAll(change);  // rowNum, colName, transformation, original, newValue
                report.add(infoRow);
            }
        }

        return report;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // EXTRACT FLAG REPORT  (convenience overloads)
    // These mirror the overloads that existed on DataCleaner pre-split.
    // ─────────────────────────────────────────────────────────────────────────

    public List<List<String>> extractFlagReport(List<List<String>> data,
                                                Set<String> identifierColumns,
                                                Map<String, List<Integer>> flagRowsByType) {
        return extractFlagReport(data, identifierColumns, flagRowsByType,
                true, null, null, null, Collections.emptyList());
    }

    public List<List<String>> extractFlagReport(List<List<String>> data,
                                                Set<String> identifierColumns) {
        return extractFlagReport(data, identifierColumns, null,
                true, null, null, null, Collections.emptyList());
    }

    public List<List<String>> extractFlagReport(List<List<String>> data) {
        return extractFlagReport(data, null, null,
                true, null, null, null, Collections.emptyList());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // BUILD SUMMARY
    //
    // Produces the two-column summary rows written to output_summary.csv/xlsx.
    // ─────────────────────────────────────────────────────────────────────────
    public List<List<String>> buildSummary(
            int originalRowCount,
            int finalRowCount,
            int rejectedRowCount,
            int multilineFixCount,
            Map<String, List<Integer>> flagRowsByType,
            String flagReportFileName) {

        List<List<String>> summary = new ArrayList<>();
        Set<Integer> allFlaggedRows  = new HashSet<>();
        int invalidCount = 0, suspiciousCount = 0, warningCount = 0, totalFlags = 0;

        for (Map.Entry<String, List<Integer>> entry : flagRowsByType.entrySet()) {
            String type        = entry.getKey();
            List<Integer> rows = entry.getValue();
            allFlaggedRows.addAll(rows);
            totalFlags += rows.size();
            if      (type.startsWith("INVALID"))     invalidCount    += rows.size();
            else if (type.startsWith("SUSPICIOUS"))  suspiciousCount += rows.size();
            else                                      warningCount    += rows.size();
        }

        int rowsWithIssues = allFlaggedRows.size();
        int cleanRows      = finalRowCount - rowsWithIssues;

        summary.add(row2("DATASET SUMMARY", ""));
        summary.add(e());
        if (rejectedRowCount > 0)
            summary.add(row2("Total Rows", originalRowCount + " → " + finalRowCount
                    + " (" + rejectedRowCount + " removed)"));
        else
            summary.add(row2("Total Rows", String.valueOf(finalRowCount)));
        summary.add(row2("Clean Rows",       String.valueOf(cleanRows)));
        summary.add(row2("Rows with Issues", String.valueOf(rowsWithIssues)));
        summary.add(row2("Removed Rows",     String.valueOf(rejectedRowCount)));
        if (multilineFixCount > 0)
            summary.add(row2("Multiline Cells Flattened", String.valueOf(multilineFixCount)));
        summary.add(e());
        summary.add(row2("Total Flags",      String.valueOf(totalFlags)));
        summary.add(row2("Invalid Flags",    String.valueOf(invalidCount)));
        summary.add(row2("Suspicious Flags", String.valueOf(suspiciousCount)));
        summary.add(row2("Warning Flags",    String.valueOf(warningCount)));
        summary.add(e());
        summary.add(row2("FLAG DETAILS", "See: " + flagReportFileName));
        summary.add(e());

        // List each flag type with its count and affected rows, grouped by severity
        List<String> ordered = new ArrayList<>();
        List<String> susp    = new ArrayList<>();
        List<String> warn    = new ArrayList<>();
        for (String type : flagRowsByType.keySet()) {
            if      (type.startsWith("INVALID"))     ordered.add(type);
            else if (type.startsWith("SUSPICIOUS"))  susp.add(type);
            else                                      warn.add(type);
        }
        ordered.addAll(susp);
        ordered.addAll(warn);

        for (String type : ordered) {
            List<Integer> rows = flagRowsByType.get(type);
            if (rows == null || rows.isEmpty()) continue;
            summary.add(row2(type + ":  " + rows.size(),
                    "→ See Flag Report (Rows: " + buildRowReference(rows) + ")"));
        }

        return summary;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // BUILD CONSISTENCY REPORT
    //
    // Formats the list of column-consistency issues returned by
    // DataCleaner.applyColumnConsistency() into printable report rows.
    // ─────────────────────────────────────────────────────────────────────────
    @SuppressWarnings("unchecked")
    public List<List<String>> buildConsistencyReport(
            List<Map<String, Object>> consistencyIssues,
            boolean normalizationEnabled) {

        List<List<String>> report = new ArrayList<>();
        if (consistencyIssues == null || consistencyIssues.isEmpty()) return report;

        report.add(Arrays.asList(
                "Column", "Accepted Values", "Unrecognized Value",
                "Count", "Rows Affected", "Status"));

        for (Map<String, Object> issue : consistencyIssues) {
            String colName  = (String) issue.get("column");
            String accepted = (String) issue.get("accepted");
            Map<String, List<Integer>> unrecognized =
                    (Map<String, List<Integer>>) issue.get("unrecognized");
            boolean firstRow = true;

            for (Map.Entry<String, List<Integer>> entry : unrecognized.entrySet()) {
                String value        = entry.getKey();
                List<Integer> rowNums = entry.getValue();
                String status = normalizationEnabled
                        ? "Flagged (add to normalization map to fix)"
                        : "Flagged (normalization disabled)";

                List<String> row = new ArrayList<>();
                row.add(firstRow ? colName : "");
                row.add(firstRow ? accepted : "");
                row.add(value);
                row.add(String.valueOf(rowNums.size()));
                row.add(buildRowReference(rowNums));
                row.add(status);
                report.add(row);
                firstRow = false;
            }
            report.add(new ArrayList<>());  // blank row between columns
        }

        return report;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PRIVATE HELPERS
    // ─────────────────────────────────────────────────────────────────────────

    /** Produces a compact row-number string, capped at MAX_SUMMARY_ROWS_LISTED. */
    private String buildRowReference(List<Integer> rows) {
        StringBuilder sb = new StringBuilder();
        int limit = Math.min(rows.size(), MAX_SUMMARY_ROWS_LISTED);
        for (int i = 0; i < limit; i++) {
            if (i > 0) sb.append(", ");
            sb.append(rows.get(i));
        }
        if (rows.size() > MAX_SUMMARY_ROWS_LISTED) sb.append("...");
        return sb.toString();
    }

    /** Creates a two-cell row for the summary table. */
    private List<String> row2(String a, String b) {
        List<String> r = new ArrayList<>();
        r.add(a);
        r.add(b);
        return r;
    }

    /** Creates an empty row (blank separator line in reports). */
    private List<String> e() {
        return new ArrayList<>();
    }

    /** Case-insensitive header column lookup. */
    private int indexOf(List<String> header, String colName) {
        for (int i = 0; i < header.size(); i++)
            if (header.get(i).equalsIgnoreCase(colName)) return i;
        return -1;
    }
}