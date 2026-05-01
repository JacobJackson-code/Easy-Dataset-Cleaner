# Easy-Dataset-Cleaner

A Java-based dataset cleaning and normalization tool built for professional data cleaning work. Reads CSV and Excel files, applies configurable cleaning and normalization rules, and outputs clean data alongside detailed flag reports, summaries, and rejected row files.

> **Note:** This project is under active development. Some features — particularly newer additions such as the time, phone, and zip normalizers, as well as scan mode, and Excel column typing — are functional but may have edge cases that have not yet been fully tested. Feedback and issue reports are welcome.

> **Development note:** This software is being developed with the assistance of AI (Claude by Anthropic). All logic, design decisions, and feature direction are human-led. AI assistance is used for code generation, which is then reviewed, tested, and refined iteratively.

---

## Features

### Input / Output
- Reads `.csv`, `.xlsx`, and `.xls` files
- Outputs clean data in the same format with colored Excel companions
- Automatically generates flag reports, summaries, rejected row files, and consistency reports
- Scan mode for pre-cleaning dataset analysis without modifying data

### Automatic Cleaning
- Whitespace trimming, empty row removal, exact duplicate removal
- Repeated header row detection and removal
- Double space and multiline cell flattening (configurable exempt columns)
- Structural validation — rows with wrong field count rejected

### Normalization
- **Numbers** — integer, decimal, rounding, currency formatting, parens notation, scientific notation control, percent
- **Names** — full name, first/last extraction, initial formatting (J. Jackson / J.Jackson), title casing
- **Dates** — intelligent multi-format parsing with preferred and secondary input order, output format control, suspicious/invalid detection, year range validation
- **Times** — 12/24 hour, seconds, timezone conversion, global timezone setting
- **Countries** — 195 countries, full name / ISO2 / ISO3 output
- **US States** — all 50 states + DC, full name / abbreviation / dotted abbreviation
- **Phone Numbers** — US and international, E.164 / national / dash / dots / digits formats
- **ZIP / Postal Codes** — US ZIP5 / ZIP+4, Canadian postal codes, UK postcodes
- **Booleans** — extensive input recognition, configurable output format
- **IDs** — prefix/suffix validation with letter, number, or literal checks
- **Email / URL validation**
- **Currency codes** — standardization and symbol conversion
- **Currency-aware decimal formatting** — per-row currency detection with correct decimal counts and regional formatting styles

### Data Quality
- Null-like value flagging with configurable values and output format
- Row quality threshold — rows below a minimum clean cell percentage are rejected
- Duplicate detection by column with quality-scored winner selection
- Required column enforcement
- Column consistency checking with optional normalization maps
- Column caps — flag values exceeding a defined maximum

### Reporting
- **Summary file** — total rows, clean rows, issues, removed rows, multiline fixes, per-flag-type row references
- **Flag report** — sorted by severity (HIGH / MEDIUM / LOW / INFO), colored severity indicator column, full original value and reason per flagged cell, silent conversion tracking at the bottom
- **Consistency report** — per-column breakdown of unrecognized values with row references
- **Rejected rows file** — every removed row with rejection reason

### Analysis (Scan Mode)
- Time column scan — detects 12hr/24hr/mixed, timezone presence, recommends format
- Number column scan — detects integer/decimal/parens/scientific, recommends rule
- Unique value scan — lists all unique values with counts, useful for setting up mappings

### Excel Output
- Flagged cells color coded (red = invalid, yellow = suspicious, blue = warnings)
- Flag colors independently toggleable from text tags
- Column type declarations — set columns as number, integer, currency, date, time, datetime, boolean, text
- Empty cells written as true BLANK type
- Row sorting by numeric column or ID segment

---

## Requirements

- Java 11 or higher
- IntelliJ IDEA (recommended) or any Java IDE
- Apache POI 5.x (Excel reading/writing)
- OpenCSV (CSV reading/writing)

### Required JAR Dependencies

**OpenCSV:**
- `opencsv`
- `commons-lang3`

**Apache POI:**
- `poi-5.x`
- `poi-ooxml-5.x`
- `poi-ooxml-full-5.x`
- `commons-collections4`
- `commons-compress`
- `xmlbeans`
- `log4j-api`
- `log4j-core`
- `commons-io`

All JARs should be added to your project's library path. In IntelliJ: `File → Project Structure → Libraries → + → Java` then select the JAR files.

---

## Installation

1. Clone or download this repository
2. Open the project in IntelliJ IDEA (or your preferred Java IDE)
3. Download the required JAR dependencies listed above
4. Add all JARs to the project library (`File → Project Structure → Libraries`)
5. Set `Main.java` as the run configuration entry point
6. Place your input file in the project root directory
7. Configure `Main.java` and run

---

## Usage

All configuration is done at the top of `Main.java`. No other files need to be modified for normal use.

```java
String inputFile  = "import.csv";   // your input file
String outputFile = "output.csv";   // your desired output file
String MODE = "CLEAN";              // "CLEAN" or "SCAN"
```

From there, uncomment and fill in whichever config sections apply to your dataset. Every option is documented with inline comments.

For a full explanation of every available option, see [`DatasetCleaner_Guide.txt`](DatasetCleaner_Guide.txt) included in the repository.

---

## Output Files

When output is `.csv`, the tool also generates `.xlsx` companions automatically.

| File | Contents |
|---|---|
| `output.csv / .xlsx` | Cleaned dataset |
| `output_summary.csv / .xlsx` | Overview with flag counts and row references |
| `output_flag_report.csv / .xlsx` | Full detail on every flagged cell |
| `output_consistency_report.csv / .xlsx` | Column consistency issues |
| `output_rejected.csv / .xlsx` | Removed rows with rejection reasons |
| `output_scan_report.csv / .xlsx` | Analysis report (SCAN mode only) |

---

## Project Structure

```
Main.java               Entry point and all configuration
DataCleaner.java        Core pipeline — orchestrates all cleaning steps
NumberNormalizer.java   Numbers, names, booleans, IDs, email/URL validation
DateNormalizer.java     Date parsing and normalization engine
TimeNormalizer.java     Time parsing and normalization
CountryNormalizer.java  Country name standardization (195 countries)
StateNormalizer.java    US state standardization (50 states + DC)
PhoneNormalizer.java    Phone number formatting and validation
ZipNormalizer.java      ZIP and postal code normalization
ScanAnalyzer.java       Pre-cleaning dataset analysis
CSVreader.java          CSV file reading with lenient fallback parser
CSVwriter.java          CSV file writing
ExcelReader.java        Excel file reading (.xlsx and .xls)
ExcelWriter.java        Excel file writing with cell typing and color coding
DatasetCleaner_Guide.txt  Full user guide covering all configuration options
```

---

## Current Limitations

- No graphical interface — all configuration is done in code
- Performance tested up to approximately 100,000 rows; very large datasets may require JVM memory tuning (`-Xmx` flag)
- Time normalization and Excel column typing are newer additions and may have untested edge cases
- Phone normalization currently focused on US numbers; international formatting is best-effort
- No undo — always keep a backup of your original file

---

## License

No license is currently specified. This means the source code is publicly viewable but may not be used, modified, or distributed without explicit permission from the author. A formal open-source license will be added in a future release.

---

## Roadmap

- [ ] Graphical user interface
- [ ] Open-source license
- [ ] Expanded international phone support
- [ ] Additional postal code formats
- [ ] Performance profiling for very large datasets
- [ ] Automated test suite

---

*Built for professional data cleaning work. Developed with AI assistance (Claude by Anthropic) under human direction and review.*
