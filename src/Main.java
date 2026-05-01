import java.util.*;

public class Main {
    public static void main(String[] args) {

        CSVreader csvReader     = new CSVreader();
        CSVwriter csvWriter     = new CSVwriter();
        ExcelReader excelReader = new ExcelReader();
        ExcelWriter excelWriter = new ExcelWriter();
        DataCleaner cleaner     = new DataCleaner();

        String inputFile  = "messyDataset.csv";
        String outputFile = "output.csv";

        // ── Mode ─────────────────────────────────────────────────────────────
        // "CLEAN" = normal cleaning and normalization (default)
        // "SCAN"  = analysis only, no data modified, produces scan report
        String MODE = "CLEAN";
        // ─────────────────────────────────────────────────────────────────────

        // ── Scan Config (used when MODE = "SCAN") ─────────────────────────────
        // colName → scan type: "time" / "number" / "unique"
        //
        // time   → detects 12hr/24hr/mixed, timezone, hour-only, recommends format
        // number → detects integer/decimal/parens/scientific, recommends rule
        // unique → lists all unique values with counts, good for mapping setup
        Map<String, String> scanColumns = new LinkedHashMap<>();
        // scanColumns.put("EventTime", "time");
        // scanColumns.put("Amount",    "number");
        // scanColumns.put("Status",    "unique");
        // scanColumns.put("Console",   "unique");
        // ─────────────────────────────────────────────────────────────────────

        // ── Scientific Notation ───────────────────────────────────────────────
        boolean allowScientificNotation = false;
        cleaner.setAllowScientificNotation(allowScientificNotation);
        // ─────────────────────────────────────────────────────────────────────

        // ── Client Column Rules ───────────────────────────────────────────────
        // NUMBERS:  integer / words / decimal:2 / round:up / round:down /
        //           round:nearest / currency:USD / percent / percent:1 /
        //           :nocomma / :standard / :parens
        // TEXT:     case:upper / case:lower / case:title / case:sentence
        //           name:full / name:first / name:last
        //           name:initial_last (J. Jackson) / name:initialnodot_last (J.Jackson)
        //           currency_code:upper / currency_code:lower / currency_code:symbol
        // VALIDATE: validate:email / validate:url / validate:any
        // ID:       id:startswith:letter / id:startswith:USR / id:endswith:number
        // BOOLEAN:  bool:true/false / bool:yes/no / bool:1/0 etc.
        Map<String, String> columnRules = new HashMap<>();
        // columnRules.put("Age",        "integer");
        // columnRules.put("Salary",     "currency:USD");
        // columnRules.put("Status",     "case:upper");
           columnRules.put("Currency",   "currency_code:upper");
           columnRules.put("Email",      "validate:email");
           columnRules.put("FullName",       "name:full");
        // columnRules.put("CustomerID", "id:startswith:USR:endswith:number");
           columnRules.put("Active",     "bool:TRUE/FALSE");
           columnRules.put("OrderAmount",     "decimal:2:standard:nocomma");
        // ─────────────────────────────────────────────────────────────────────

        // ── Client Column Caps ────────────────────────────────────────────────
        Map<String, Long> columnCaps = new HashMap<>();
        // columnCaps.put("Salary", 500000L);
        // ─────────────────────────────────────────────────────────────────────

        // ── Client Column Mappings ────────────────────────────────────────────
        Map<String, Map<String, String>> columnMappings = new HashMap<>();
        // Map<String, String> statusMap = new HashMap<>();
        // statusMap.put("active", "Active"); statusMap.put("inactive", "Inactive");
        // columnMappings.put("Status", statusMap);
        // ─────────────────────────────────────────────────────────────────────

        // ── Client Country Config ─────────────────────────────────────────────
        Map<String, String> countryColumns = new HashMap<>();
           countryColumns.put("BirthCountry", "fullname");
        // ─────────────────────────────────────────────────────────────────────

        // ── US State Normalization ────────────────────────────────────────────
        // options: "abbr" → CA  / "full" → California  / "abbr_dot" → C.A.
        // Recognizes: full names, abbreviations, common variants, dotted forms
        Map<String, String> stateColumns = new HashMap<>();
           stateColumns.put("State", "abbr");
        // stateColumns.put("StateOfResidence", "full");
        // ─────────────────────────────────────────────────────────────────────

        // ── Time Normalization ────────────────────────────────────────────────
        // timeColumns: column → {outputFormat, timezoneHandling}
        //
        // Output formats:
        //   "HH:mm"       24-hour no seconds         15:30
        //   "HH:mm:ss"    24-hour with seconds        15:30:45
        //   "hh:mm a"     12-hour no seconds          3:30 PM
        //   "hh:mm:ss a"  12-hour with seconds        3:30:45 PM
        //
        // Timezone handling:
        //   "strip"              remove timezone info entirely
        //   "preserve"           keep original timezone from input
        //   "UTC"                convert to UTC
        //   "America/New_York"   convert to any valid Java zone ID
        //   "America/Chicago"    "America/Los_Angeles" "Europe/London" etc.
        //
        // globalTimezone: assumed source timezone for times with NO timezone info
        //   null = no assumption, bare times like "14:30" are used as-is
        //   "America/Chicago" = all bare times treated as CST
        Map<String, String[]> timeColumns = new HashMap<>();
           timeColumns.put("SignupTime",  new String[]{"HH:mm:ss"});
           //timeColumns.put("SignupTime",  new String[]{"HH:mm:ss", "UTC"});
        // timeColumns.put("OrderTime",  new String[]{"hh:mm a",  "America/New_York"});
        // timeColumns.put("LocalTime",  new String[]{"HH:mm",    "strip"});

        String globalTimezone = null; // e.g. "America/Chicago" or null
        // ─────────────────────────────────────────────────────────────────────
        Map<String, String[]> phoneColumns = new LinkedHashMap<>();
           phoneColumns.put("Phone", new String[]{"national", "US"});
        // phoneColumns.put("Phone", new String[]{"national", "US"});

        Map<String, String[]> zipColumns = new LinkedHashMap<>();
          // zipColumns.put("ZipCode", new String[]{"zip5", "US"});

        String nullOutputFormat = null;
        // nullOutputFormat = "N/A";

        // ── Client Date Config ────────────────────────────────────────────────
        // Format: {"primaryOrder", "outputFormat"}
        //      or {"primaryOrder", "outputFormat", "secondaryOrder"}
        //
        // primaryOrder:   MDY / DMY / YMD — preferred input format
        // outputFormat:   "MM/dd/yyyy" / "dd/MM/yyyy" / "yyyy-MM-dd" / "MMMM d yyyy"
        // secondaryOrder: optional — most common format in the dataset
        //   When two valid interpretations exist, secondary breaks the tie confidently
        //   instead of flagging as suspicious. Set if you know the client's data source.
        //   Example: primary=YMD output=yyyy-MM-dd secondary=MDY (American data)
        Map<String, String[]> dateColumns = new HashMap<>();
           dateColumns.put("SignupDate", new String[]{"YMD", "yyyy-MM-dd", "MDY"});
        // dateColumns.put("DOB",        new String[]{"DMY", "dd/MM/yyyy"});
        int minYear             = 1900;
        int maxYear             = 2100;
        int suspiciousYearBefore = 0;
        // ─────────────────────────────────────────────────────────────────────

        // ── Currency-Aware Decimal Formatting ─────────────────────────────────
        // "standard" / "nogrouping" / "decimal"
        Map<String, String> currencyAwareColumns = new HashMap<>();
        // currencyAwareColumns.put("Amount", "Currency");
        String currencyAwareFormat = "standard";
        // ─────────────────────────────────────────────────────────────────────

        // ── Structural Validation ─────────────────────────────────────────────
        // false = rows with wrong column count are rejected (default, safe)
        // true  = rows with wrong column count are kept as-is
        boolean allowStructuralMismatch = false;
        // ─────────────────────────────────────────────────────────────────────

        // ── Required Columns ─────────────────────────────────────────────────
        // Any row missing a value in these columns goes to the rejected file
        Set<String> requiredColumns = new HashSet<>();
           requiredColumns.add("CustomerID");
        // requiredColumns.add("Email");
        // ─────────────────────────────────────────────────────────────────────

        // ── Column Consistency Check ──────────────────────────────────────────
        // Define accepted values per column.
        // Any value not in the accepted list (or normalization map) is reported.
        Map<String, Set<String>> consistencyColumns = new HashMap<>();

        // Example:
        // Set<String> consoleValues = new HashSet<>(Arrays.asList(
        //         "PS5", "Xbox Series X", "Nintendo Switch", "PC"));
        // consistencyColumns.put("Console", consoleValues);

        // Optional normalization maps per column.
        // Known variants get silently corrected when normalization is enabled.
        Map<String, Map<String, String>> consistencyNormalizers = new HashMap<>();

        // Example:
        // Map<String, String> consoleNorm = new HashMap<>();
        // consoleNorm.put("playstation 5",  "PS5");
        // consoleNorm.put("playstation5",   "PS5");
        // consoleNorm.put("ps 5",           "PS5");
        // consoleNorm.put("xbox series x",  "Xbox Series X");
        // consoleNorm.put("xsx",            "Xbox Series X");
        // consistencyNormalizers.put("Console", consoleNorm);

        // true  = fix known variants silently, report unknowns
        // false = report only, no changes made (good for Basic tier review)
        boolean applyConsistencyNormalization = true;
        // ─────────────────────────────────────────────────────────────────────

        // ── Duplicate Detection ───────────────────────────────────────────────
        Set<String> duplicateColumns = new HashSet<>();
           duplicateColumns.add("CustomerID");
        // ─────────────────────────────────────────────────────────────────────

        // ── Null-Like Value Flags ─────────────────────────────────────────────
        Set<String> nullFlags = new HashSet<>();
        nullFlags.add("none");   nullFlags.add("null");    nullFlags.add("na");
        nullFlags.add("n/a");    nullFlags.add("-");        nullFlags.add("unknown");
        nullFlags.add("tbd");    nullFlags.add("nil");      nullFlags.add("undefined");
        nullFlags.add("?");
        // ─────────────────────────────────────────────────────────────────────

        // ── Double Space Fix (Allow) ──────────────────────────────────────────────────
        Set<String> allowDoubleSpaces = new HashSet<>();
        // allowDoubleSpaces.add("Notes");
        // ─────────────────────────────────────────────────────────────────────

        // ── Multiline Fix (Allow)─────────────────────────────────────────────────────
        Set<String> allowMultiline = new HashSet<>();
        // allowMultiline.add("Notes");
        // ─────────────────────────────────────────────────────────────────────

        // ── Row Quality Threshold ─────────────────────────────────────────────
        double minRowQuality = 0.6;
        // ─────────────────────────────────────────────────────────────────────

        // ── Flag Report Identifier Columns ────────────────────────────────────
        Set<String> flagReportIdentifiers = new LinkedHashSet<>();
           flagReportIdentifiers.add("CustomerID");
        // flagReportIdentifiers.add("Email");
        // ─────────────────────────────────────────────────────────────────────

        // ── Excel Column Types ───────────────────────────────────────────────────
        // Sets the actual Excel cell type for each column in .xlsx output.
        // CSV output is unaffected — always plain text.
        // Flagged cells always stay as text regardless of declared type.
        // Empty cells always write as BLANK regardless of declared type.
        //
        // Supported types:
        //   number    → numeric  e.g. 1,234.56
        //   integer   → numeric  e.g. 1,234
        //   currency  → numeric  e.g. $1,234.56
        //   percent   → numeric  e.g. 45.50%  (store value as 45.5, not 0.455)
        //   date      → numeric with date format  e.g. 2024-03-04
        //   time      → numeric with time format  e.g. 15:30:00
        //   datetime  → numeric with datetime     e.g. 2024-03-04 15:30
        //   boolean   → native Excel boolean (TRUE/FALSE)
        //   text      → explicit string
        Map<String, String> excelColumnTypes = new HashMap<>();
           excelColumnTypes.put("OrderAmount",    "currency");
           excelColumnTypes.put("Score",       "integer");
        // excelColumnTypes.put("Salary",    "currency");
        // excelColumnTypes.put("Rate",      "percent");
           excelColumnTypes.put("SignupDate",      "date");
           excelColumnTypes.put("SignupTime", "time");
        // excelColumnTypes.put("UpdatedAt", "datetime");
           excelColumnTypes.put("Active",    "boolean");
           excelColumnTypes.put("Notes",     "text");
        // excelColumnTypes.put("Notes",     "text");
        // ─────────────────────────────────────────────────────────────────────

        // ── Row Sort ──────────────────────────────────────────────────────────
        // Sort the final cleaned rows by a column before writing output.
        // sortColumn:  column name to sort by (null = no sorting)
        // sortOrder:   "asc" (lowest first) or "desc" (highest first)
        // sortExtract: how to extract sort key from cell value
        //   null           = parse full value as number
        //   "last_number"  = last numeric segment  USR-0042 → 42
        //   "numeric_only" = strip non-digits      USR-0042 → 42
        //   "position:N"   = Nth segment (1-based) USR-2024-0042, position:3 → 0042
        String sortColumn  = null;   // e.g. "Amount" or "CustomerID"
        String sortOrder   = "desc"; // "asc" or "desc"
        String sortExtract = null;   // null, "last_number", "numeric_only", "position:2"
        // ─────────────────────────────────────────────────────────────────────

        // ── Flag Visibility in Clean Output ───────────────────────────────────
        // Controls what appears in the CLEAN OUTPUT FILE.
        // Flag report always contains full detail regardless of these settings.

        // false = no flags shown in clean file, original values restored
        // true  = flagged cells show [FLAG_TYPE] originalValue (default)
        boolean showFlagsInOutput = true;

        // false = no cell background colors in Excel output
        // true  = flagged cells get color coding (red/yellow/blue) (default)
        // Works independently of showFlagsInOutput:
        //   both true  → tag + color
        //   text only  → showFlagsInOutput=true,  showFlagColors=false
        //   color only → showFlagsInOutput=false, showFlagColors=true
        //   both off   → original values, plain cells
        boolean showFlagColors = true;

        // Add specific flag types to hide from clean output (show original value)
        // Flag still appears in flag report — just not visible in clean data file
        // Options: INVALID_DATE, INVALID_NUMBER, INVALID_EMAIL, INVALID_URL,
        //          INVALID_CONTACT, INVALID_NAME, INVALID_ID, INVALID_BOOL,
        //          INVALID_CURRENCY, SUSPICIOUS_DATE, NULL_VALUE, CAP_EXCEEDED
        Set<String> hiddenFlagTypes = new HashSet<>();
        // hiddenFlagTypes.add("SUSPICIOUS_DATE");  // dates still in report, clean in file
        // hiddenFlagTypes.add("NULL_VALUE");        // null flags hidden from clean file
           hiddenFlagTypes.add("INVALID_NAME");
        // ─────────────────────────────────────────────────────────────────────

        // ── Scan mode — runs instead of clean when MODE = "SCAN" ─────────────
        if (MODE.equalsIgnoreCase("SCAN")) {
            List<List<String>> scanData;
            String scanInputLower = inputFile.toLowerCase();
            if (scanInputLower.endsWith(".xlsx") || scanInputLower.endsWith(".xls")) {
                scanData = new ExcelReader().read(inputFile);
            } else {
                scanData = new CSVreader().read(inputFile);
            }
            if (!scanData.isEmpty()) {
                ScanAnalyzer analyzer = new ScanAnalyzer();
                List<List<String>> scanReport = analyzer.scan(scanData, scanColumns);
                String scanOutputBase = buildName(outputFile, "_scan_report");
                writeOutput(scanOutputBase, scanReport, csvWriter, excelWriter);
                System.out.println("Scan complete → " + scanOutputBase);
            }
            return; // exit — no cleaning done in SCAN mode
        }

        // ── Read ──────────────────────────────────────────────────────────────
        List<List<String>> data;
        String inputLower = inputFile.toLowerCase();
        if (inputLower.endsWith(".xlsx") || inputLower.endsWith(".xls")) {
            System.out.println("Detected Excel input file...");
            data = excelReader.read(inputFile);
        } else if (inputLower.endsWith(".csv")) {
            System.out.println("Detected CSV input file...");
            data = csvReader.read(inputFile);
        } else {
            System.out.println("Unsupported input type. Please use .csv, .xlsx, or .xls");
            return;
        }

        // Capture original row count before any processing
        int originalRowCount = data.isEmpty() ? 0 : data.size() - 1;

        // Clear any silent changes from previous runs
        cleaner.clearSilentChanges();

        // ── Clean ─────────────────────────────────────────────────────────────
        List<List<String>> cleaned = cleaner.clean(data);

        // ── Apply all rules ───────────────────────────────────────────────────
        List<List<String>> rejectedRows = new ArrayList<>();

        if (!cleaned.isEmpty()) {
            List<String> header = cleaned.get(0);
            List<List<String>> rows = new ArrayList<>(cleaned.subList(1, cleaned.size()));

            // Step 1: Structural validation
            rows = cleaner.applyStructuralValidation(header, rows, rejectedRows, allowStructuralMismatch);

            // Step 2: Required columns
            rows = cleaner.applyRequiredColumns(header, rows, requiredColumns, rejectedRows);

            // Step 3: Null flagging and formatting fixes
            rows = cleaner.applyNullFlagDetection(rows, nullFlags);
            rows = cleaner.applyDoubleSpaceFix(rows, allowDoubleSpaces, header);
            rows = cleaner.applyMultilineFix(rows, allowMultiline, header);

            // Step 4: Column transformations
            rows = cleaner.applyColumnRules(header, rows, columnRules);
            rows = cleaner.applyColumnMappings(header, rows, columnMappings);
            rows = cleaner.applyCountryNormalization(header, rows, countryColumns);
            rows = cleaner.applyStateNormalization(header, rows, stateColumns);
            rows = cleaner.applyTimeNormalization(header, rows, timeColumns, globalTimezone);
            rows = cleaner.applyPhoneNormalization(header, rows, phoneColumns);
            rows = cleaner.applyZipNormalization(header, rows, zipColumns);
            rows = cleaner.applyDateNormalization(header, rows, dateColumns,
                    suspiciousYearBefore, minYear, maxYear);
            rows = cleaner.applyColumnCaps(header, rows, columnCaps, columnRules);
            rows = cleaner.applyCurrencyAwareDecimals(header, rows,
                    currencyAwareColumns, currencyAwareFormat);

            // Step 5: Duplicate detection (after transforms so values are normalized)
            rows = cleaner.applyDuplicateDetection(header, rows,
                    duplicateColumns, rejectedRows);

            // Step 6: Row quality check (after all flags applied)
            if (minRowQuality > 0.0)
                rows = cleaner.applyRowQualityCheck(rows, minRowQuality, rejectedRows);

            cleaned = new ArrayList<>();
            cleaned.add(header);
            cleaned.addAll(rows);
        }

        // ── Column consistency check ──────────────────────────────────────────
        // Runs on final cleaned data — after normalization so values are clean
        List<Map<String, Object>> consistencyIssues = null;
        if (!consistencyColumns.isEmpty() && !cleaned.isEmpty()) {
            List<String> header = cleaned.get(0);
            List<List<String>> rows = new ArrayList<>(cleaned.subList(1, cleaned.size()));
            consistencyIssues = cleaner.applyColumnConsistency(
                    header, rows, consistencyColumns,
                    consistencyNormalizers, applyConsistencyNormalization);
            cleaned = new ArrayList<>();
            cleaned.add(header);
            cleaned.addAll(rows);
        }

        // ── Sort rows ─────────────────────────────────────────────────────────
        if (sortColumn != null && !cleaned.isEmpty()) {
            List<String> header = cleaned.get(0);
            List<List<String>> rows = new ArrayList<>(cleaned.subList(1, cleaned.size()));
            rows = cleaner.applySortRows(header, rows, sortColumn, sortOrder, sortExtract);
            cleaned = new ArrayList<>();
            cleaned.add(header);
            cleaned.addAll(rows);
        }

        // ── Extract flag report — also shortens flags in cleaned data ─────────
        Map<String, List<Integer>> flagRowsByType = new LinkedHashMap<>();
        Map<String, String> cellColorMap = new LinkedHashMap<>();
        List<List<String>> flagReport = cleaner.extractFlagReport(
                cleaned, flagReportIdentifiers, flagRowsByType,
                showFlagsInOutput, hiddenFlagTypes, cellColorMap, nullOutputFormat);
        boolean hasFlaggedCells = flagReport.size() > 1;

        // ── Write main output ─────────────────────────────────────────────────
        // Get header for column type lookup
        List<String> finalHeader = cleaned.isEmpty() ? null : cleaned.get(0);
        writeOutputFull(outputFile, cleaned, csvWriter, excelWriter,
                showFlagColors ? cellColorMap : null,
                excelColumnTypes, finalHeader, showFlagColors);
        System.out.println("Clean data → " + outputFile);

        // ── Write flag report ─────────────────────────────────────────────────
        if (hasFlaggedCells) {
            String flagReportBase = buildName(outputFile, "_flag_report");
            // Flag report uses special writer that colors only the Severity column
            writeFlagReportOutput(flagReportBase, flagReport, csvWriter, excelWriter);
            System.out.println((flagReport.size()-1) + " flagged cell(s) → " + flagReportBase);

            // Write summary
            int finalRowCount = cleaned.isEmpty() ? 0 : cleaned.size() - 1;
            String flagReportDisplay = buildDisplayName(outputFile, "_flag_report");
            List<List<String>> summary = cleaner.buildSummary(
                    originalRowCount, finalRowCount,
                    rejectedRows.size(), cleaner.getMultilineFixCount(),
                    flagRowsByType, flagReportDisplay);
            String summaryBase = buildName(outputFile, "_summary");
            writeOutput(summaryBase, summary, csvWriter, excelWriter);
            System.out.println("Summary → " + summaryBase);
        } else {
            System.out.println("No flagged cells.");
        }

        // ── Write consistency report ──────────────────────────────────────────
        if (consistencyIssues != null && !consistencyIssues.isEmpty()) {
            List<List<String>> consistencyReport = cleaner.buildConsistencyReport(
                    consistencyIssues, applyConsistencyNormalization);
            String consistencyBase = buildName(outputFile, "_consistency_report");
            writeOutput(consistencyBase, consistencyReport, csvWriter, excelWriter);
            System.out.println(consistencyIssues.size()
                    + " column(s) with consistency issues → " + consistencyBase);
        } else if (!consistencyColumns.isEmpty()) {
            System.out.println("No consistency issues found.");
        }

        // ── Write rejected rows ───────────────────────────────────────────────
        if (!rejectedRows.isEmpty()) {
            List<List<String>> rejectedOutput = new ArrayList<>();
            List<String> rejectedHeader = new ArrayList<>();
            rejectedHeader.add("Rejection Reason");
            if (!cleaned.isEmpty()) rejectedHeader.addAll(cleaned.get(0));
            rejectedOutput.add(rejectedHeader);
            rejectedOutput.addAll(rejectedRows);
            String rejectedBase = buildName(outputFile, "_rejected");
            writeOutput(rejectedBase, rejectedOutput, csvWriter, excelWriter);
            System.out.println(rejectedRows.size() + " rejected row(s) → " + rejectedBase);
        } else {
            System.out.println("No rejected rows.");
        }

        System.out.println("Cleaning complete. Output saved to " + outputFile);
    }

    // ── Full output helper — passes color map and column types to ExcelWriter ──
    private static void writeOutputFull(String fileNameOrBase,
                                        List<List<String>> data,
                                        CSVwriter csvWriter,
                                        ExcelWriter excelWriter,
                                        Map<String, String> cellColorMap,
                                        Map<String, String> columnTypes,
                                        List<String> header,
                                        boolean showFlagColors) {
        String lower = fileNameOrBase.toLowerCase();
        if (lower.endsWith(".csv")) {
            csvWriter.write(fileNameOrBase, data);
            excelWriter.write(swapExt(fileNameOrBase, ".xlsx"), data,
                    cellColorMap, columnTypes, header, showFlagColors);
        } else if (lower.endsWith(".xls") && !lower.endsWith(".xlsx")) {
            excelWriter.write(fileNameOrBase, data,
                    cellColorMap, columnTypes, header, showFlagColors);
            excelWriter.write(swapExt(fileNameOrBase, ".xlsx"), data,
                    cellColorMap, columnTypes, header, showFlagColors);
        } else {
            excelWriter.write(fileNameOrBase, data,
                    cellColorMap, columnTypes, header, showFlagColors);
        }
    }

    // ── Flag report output helper ────────────────────────────────────────────
    private static void writeFlagReportOutput(String fileNameOrBase,
                                              List<List<String>> data,
                                              CSVwriter csvWriter,
                                              ExcelWriter excelWriter) {
        String lower = fileNameOrBase.toLowerCase();
        if (lower.endsWith(".csv")) {
            csvWriter.write(fileNameOrBase, data);
            excelWriter.writeFlagReport(swapExt(fileNameOrBase, ".xlsx"), data);
        } else if (lower.endsWith(".xls") && !lower.endsWith(".xlsx")) {
            excelWriter.writeFlagReport(fileNameOrBase, data);
            excelWriter.writeFlagReport(swapExt(fileNameOrBase, ".xlsx"), data);
        } else {
            excelWriter.writeFlagReport(fileNameOrBase, data);
        }
    }

    // ── Output helper ─────────────────────────────────────────────────────────
    private static void writeOutput(String fileNameOrBase,
                                    List<List<String>> data,
                                    CSVwriter csvWriter,
                                    ExcelWriter excelWriter) {
        String lower = fileNameOrBase.toLowerCase();
        if (lower.endsWith(".csv")) {
            csvWriter.write(fileNameOrBase, data);
            excelWriter.write(swapExt(fileNameOrBase, ".xlsx"), data);
        } else if (lower.endsWith(".xls") && !lower.endsWith(".xlsx")) {
            excelWriter.write(fileNameOrBase, data);
            excelWriter.write(swapExt(fileNameOrBase, ".xlsx"), data);
        } else if (lower.endsWith(".xlsx")) {
            excelWriter.write(fileNameOrBase, data);
        } else {
            csvWriter.write(fileNameOrBase + ".csv", data);
            excelWriter.write(fileNameOrBase + ".xlsx", data);
        }
    }

    private static String buildName(String outputFile, String suffix) {
        int dot = outputFile.lastIndexOf(".");
        if (dot == -1) return outputFile + suffix;
        return outputFile.substring(0, dot) + suffix + outputFile.substring(dot);
    }

    private static String buildDisplayName(String outputFile, String suffix) {
        String full = buildName(outputFile, suffix);
        int slash = Math.max(full.lastIndexOf("/"), full.lastIndexOf("\\"));
        return slash >= 0 ? full.substring(slash+1) : full;
    }

    private static String swapExt(String filename, String newExt) {
        int dot = filename.lastIndexOf(".");
        if (dot == -1) return filename + newExt;
        return filename.substring(0, dot) + newExt;
    }
}