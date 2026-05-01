import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.*;
import org.apache.poi.hssf.usermodel.*;
import org.apache.poi.hssf.util.HSSFColor;
import org.apache.poi.ss.usermodel.CellType;
import java.io.*;
import java.time.*;
import java.time.format.*;
import java.util.*;

public class ExcelWriter {

    // ── Flag color hex values ─────────────────────────────────────────────────
    private static final String RED_HEX    = "FFE6E6";
    private static final String YELLOW_HEX = "FFFACD";
    private static final String BLUE_HEX   = "DDEEFF";
    private static final String GRAY_HEX   = "F0F0F0";

    // ── Excel format strings per declared type ────────────────────────────────
    private static final Map<String, String> EXCEL_FORMATS = new LinkedHashMap<>();
    static {
        EXCEL_FORMATS.put("number",   "#,##0.00");
        EXCEL_FORMATS.put("integer",  "#,##0");
        EXCEL_FORMATS.put("currency", "$#,##0.00");
        EXCEL_FORMATS.put("percent",  "0.00%");
        EXCEL_FORMATS.put("date",     "yyyy-mm-dd");
        EXCEL_FORMATS.put("time",     "hh:mm:ss");
        EXCEL_FORMATS.put("datetime", "yyyy-mm-dd hh:mm");
    }

    // ── Flag content prefixes for detection when no color map provided ────────
    private static final String[] FLAG_STARTS = {
            "[INVALID_", "[SUSPICIOUS_", "[NULL_VALUE]", "[CAP_EXCEEDED]",
            "[REJECTED]", "[INVALID_TIME]", "[INVALID_STATE]"
    };

    // ─────────────────────────────────────────────────────────────────────────
    // PUBLIC WRITE — simple overload for backward compatibility
    // ─────────────────────────────────────────────────────────────────────────
    public void write(String filePath, List<List<String>> data) {
        write(filePath, data, null, null, null, true);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PUBLIC WRITE — full signature
    // cellColorMap:  "rowIdx:colIdx" → flagType  (null = detect from cell content)
    // columnTypes:   colName → type string        (null = all STRING)
    // header:        needed for columnTypes lookup
    // showFlagColors: false = no cell background colors
    // ─────────────────────────────────────────────────────────────────────────
    public void write(String filePath, List<List<String>> data,
                      Map<String, String> cellColorMap,
                      Map<String, String> columnTypes,
                      List<String> header,
                      boolean showFlagColors) {
        if (filePath.toLowerCase().endsWith(".xlsx")) {
            writeXlsx(filePath, data, cellColorMap, columnTypes, header, showFlagColors);
        } else {
            writeXls(filePath, data, cellColorMap, columnTypes, header, showFlagColors);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // XLSX WRITER
    // ─────────────────────────────────────────────────────────────────────────
    private void writeXlsx(String filePath, List<List<String>> data,
                           Map<String, String> cellColorMap,
                           Map<String, String> columnTypes,
                           List<String> header,
                           boolean showFlagColors) {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            XSSFSheet sheet = workbook.createSheet("Cleaned Data");

            XSSFCellStyle redStyle    = showFlagColors ? makeXlsxStyle(workbook, RED_HEX)    : null;
            XSSFCellStyle yellowStyle = showFlagColors ? makeXlsxStyle(workbook, YELLOW_HEX) : null;
            XSSFCellStyle blueStyle   = showFlagColors ? makeXlsxStyle(workbook, BLUE_HEX)   : null;
            XSSFCellStyle headerStyle = makeXlsxHeaderStyle(workbook);
            XSSFCellStyle plainStyle  = workbook.createCellStyle();

            // Build column index → type map
            Map<Integer, String> colTypeByIndex = new HashMap<>();
            if (columnTypes != null && header != null) {
                for (int i = 0; i < header.size(); i++) {
                    String type = columnTypes.get(header.get(i));
                    if (type != null) colTypeByIndex.put(i, type.toLowerCase());
                }
            }

            // Style cache — avoids hitting Excel's style limit
            Map<String, XSSFCellStyle> typeStyleCache = new HashMap<>();

            for (int i = 0; i < data.size(); i++) {
                XSSFRow row = sheet.createRow(i);
                List<String> rowData = data.get(i);

                for (int j = 0; j < rowData.size(); j++) {
                    XSSFCell cell = row.createCell(j);
                    String value  = rowData.get(j);

                    // ── Header row — always STRING, always header style ────────
                    if (i == 0) {
                        cell.setCellValue(value != null ? value : "");
                        cell.setCellStyle(headerStyle);
                        continue;
                    }

                    // ── Empty / null → BLANK ──────────────────────────────────
                    if (value == null || value.trim().isEmpty()) {
                        cell.setBlank();
                        cell.setCellStyle(plainStyle);
                        continue;
                    }

                    // ── Determine if flagged ──────────────────────────────────
                    String flagType = null;
                    if (cellColorMap != null) {
                        flagType = cellColorMap.get(i + ":" + j);
                    } else if (isFlaggedContent(value)) {
                        flagType = detectFlagType(value);
                    }

                    // ── Flagged → STRING + optional color ─────────────────────
                    if (flagType != null) {
                        cell.setCellValue(value);
                        cell.setCellType(CellType.STRING);
                        if (showFlagColors) {
                            XSSFCellStyle cs = getFlagColorStyleXlsx(workbook, flagType,
                                    redStyle, yellowStyle, blueStyle);
                            cell.setCellStyle(cs != null ? cs : plainStyle);
                        } else {
                            cell.setCellStyle(plainStyle);
                        }
                        continue;
                    }

                    // ── Typed column — try to set correct Excel type ──────────
                    String type = colTypeByIndex.get(j);
                    if (type != null) {
                        boolean set = setCellTyped(cell, value, type, workbook,
                                typeStyleCache, plainStyle);
                        if (set) continue;
                        // Could not parse to declared type — fall through to STRING
                    }

                    // ── Default — plain STRING ────────────────────────────────
                    cell.setCellValue(value);
                    cell.setCellStyle(plainStyle);
                }
            }

            if (!data.isEmpty())
                for (int i = 0; i < data.get(0).size(); i++) sheet.autoSizeColumn(i);
            try (FileOutputStream fos = new FileOutputStream(filePath)) {
                workbook.write(fos);
            }
        } catch (Exception e) {
            System.out.println("Error writing Excel file: " + e.getMessage());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // SET CELL WITH CORRECT EXCEL TYPE
    // Returns true if successfully set, false → caller falls back to STRING
    // ─────────────────────────────────────────────────────────────────────────
    private boolean setCellTyped(XSSFCell cell, String value, String type,
                                 XSSFWorkbook workbook,
                                 Map<String, XSSFCellStyle> styleCache,
                                 XSSFCellStyle plainStyle) {
        try {
            switch (type) {
                case "number":
                case "currency": {
                    double d = parseDouble(value);
                    cell.setCellValue(d);
                    cell.setCellStyle(getTypeStyle(workbook, type, styleCache));
                    return true;
                }
                case "integer": {
                    double d = parseDouble(value);
                    cell.setCellValue((long) d);
                    cell.setCellStyle(getTypeStyle(workbook, type, styleCache));
                    return true;
                }
                case "percent": {
                    String cleaned = value.replace("%","").trim();
                    double d = Double.parseDouble(cleaned.replaceAll("[,$€£¥ ]","")) / 100.0;
                    cell.setCellValue(d);
                    cell.setCellStyle(getTypeStyle(workbook, type, styleCache));
                    return true;
                }
                case "date": {
                    LocalDate date = parseDate(value);
                    if (date == null) return false;
                    cell.setCellValue(date);
                    cell.setCellStyle(getTypeStyle(workbook, type, styleCache));
                    return true;
                }
                case "time": {
                    LocalTime time = parseTime(value);
                    if (time == null) return false;
                    double fraction = (time.getHour() * 3600.0
                            + time.getMinute() * 60.0
                            + time.getSecond()) / 86400.0;
                    cell.setCellValue(fraction);
                    cell.setCellStyle(getTypeStyle(workbook, type, styleCache));
                    return true;
                }
                case "datetime": {
                    LocalDateTime dt = parseDateTime(value);
                    if (dt == null) return false;
                    cell.setCellValue(dt);
                    cell.setCellStyle(getTypeStyle(workbook, type, styleCache));
                    return true;
                }
                case "boolean": {
                    Boolean b = parseBoolean(value);
                    if (b == null) return false;
                    cell.setCellValue(b);
                    return true;
                }
                case "text": {
                    cell.setCellValue(value);
                    cell.setCellStyle(plainStyle);
                    return true;
                }
                default:
                    return false;
            }
        } catch (Exception e) {
            return false; // parse failed — caller writes as STRING
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // XLS WRITER (no typed cells — XLS support is write-as-string for data cells)
    // ─────────────────────────────────────────────────────────────────────────
    private void writeXls(String filePath, List<List<String>> data,
                          Map<String, String> cellColorMap,
                          Map<String, String> columnTypes,
                          List<String> header,
                          boolean showFlagColors) {
        try (HSSFWorkbook workbook = new HSSFWorkbook()) {
            HSSFSheet sheet = workbook.createSheet("Cleaned Data");

            HSSFCellStyle redStyle    = showFlagColors
                    ? makeXlsStyle(workbook, HSSFColor.HSSFColorPredefined.ROSE.getIndex()) : null;
            HSSFCellStyle yellowStyle = showFlagColors
                    ? makeXlsStyle(workbook, HSSFColor.HSSFColorPredefined.LIGHT_YELLOW.getIndex()) : null;
            HSSFCellStyle blueStyle   = showFlagColors
                    ? makeXlsStyle(workbook, HSSFColor.HSSFColorPredefined.LIGHT_BLUE.getIndex()) : null;
            HSSFCellStyle headerStyle = makeXlsHeaderStyle(workbook);
            HSSFCellStyle plainStyle  = workbook.createCellStyle();

            for (int i = 0; i < data.size(); i++) {
                HSSFRow row = sheet.createRow(i);
                List<String> rowData = data.get(i);

                for (int j = 0; j < rowData.size(); j++) {
                    HSSFCell cell = row.createCell(j);
                    String value  = rowData.get(j);

                    if (i == 0) {
                        cell.setCellValue(value != null ? value : "");
                        cell.setCellStyle(headerStyle);
                        continue;
                    }
                    if (value == null || value.trim().isEmpty()) {
                        cell.setBlank();
                        cell.setCellStyle(plainStyle);
                        continue;
                    }

                    String flagType = null;
                    if (cellColorMap != null) {
                        flagType = cellColorMap.get(i + ":" + j);
                    } else if (isFlaggedContent(value)) {
                        flagType = detectFlagType(value);
                    }

                    cell.setCellValue(value);
                    if (flagType != null && showFlagColors) {
                        HSSFCellStyle cs = getFlagColorStyleXls(flagType,
                                redStyle, yellowStyle, blueStyle);
                        cell.setCellStyle(cs != null ? cs : plainStyle);
                    } else {
                        cell.setCellStyle(plainStyle);
                    }
                }
            }
            if (!data.isEmpty())
                for (int i = 0; i < data.get(0).size(); i++) sheet.autoSizeColumn(i);
            try (FileOutputStream fos = new FileOutputStream(filePath)) {
                workbook.write(fos);
            }
        } catch (Exception e) {
            System.out.println("Error writing Excel file: " + e.getMessage());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // WRITE FLAG REPORT — colors only the Severity column (col 0)
    // ─────────────────────────────────────────────────────────────────────────
    public void writeFlagReport(String filePath, List<List<String>> data) {
        if (filePath.toLowerCase().endsWith(".xlsx")) {
            writeFlagReportXlsx(filePath, data);
        } else {
            writeFlagReportXls(filePath, data);
        }
    }

    private void writeFlagReportXlsx(String filePath, List<List<String>> data) {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            XSSFSheet sheet   = workbook.createSheet("Flag Report");
            XSSFCellStyle hdr = makeXlsxHeaderStyle(workbook);
            XSSFCellStyle red = makeXlsxStyle(workbook, RED_HEX);
            XSSFCellStyle yel = makeXlsxStyle(workbook, YELLOW_HEX);
            XSSFCellStyle blu = makeXlsxStyle(workbook, BLUE_HEX);
            XSSFCellStyle gry = makeXlsxStyle(workbook, GRAY_HEX);
            XSSFCellStyle nrm = workbook.createCellStyle();

            for (int i = 0; i < data.size(); i++) {
                XSSFRow row = sheet.createRow(i);
                List<String> rowData = data.get(i);
                for (int j = 0; j < rowData.size(); j++) {
                    XSSFCell cell = row.createCell(j);
                    String value = rowData.get(j);
                    cell.setCellValue(value != null ? value : "");
                    if (i == 0) { cell.setCellStyle(hdr); }
                    else if (j == 0) {
                        if      ("HIGH".equals(value))   cell.setCellStyle(red);
                        else if ("MEDIUM".equals(value)) cell.setCellStyle(yel);
                        else if ("LOW".equals(value))    cell.setCellStyle(blu);
                        else if ("INFO".equals(value))   cell.setCellStyle(gry);
                        else                             cell.setCellStyle(nrm);
                    } else { cell.setCellStyle(nrm); }
                }
            }
            if (!data.isEmpty())
                for (int i = 0; i < data.get(0).size(); i++) sheet.autoSizeColumn(i);
            try (FileOutputStream fos = new FileOutputStream(filePath)) {
                workbook.write(fos);
            }
        } catch (Exception e) {
            System.out.println("Error writing flag report: " + e.getMessage());
        }
    }

    private void writeFlagReportXls(String filePath, List<List<String>> data) {
        try (HSSFWorkbook workbook = new HSSFWorkbook()) {
            HSSFSheet sheet = workbook.createSheet("Flag Report");
            HSSFCellStyle hdr = makeXlsHeaderStyle(workbook);
            HSSFCellStyle red = makeXlsStyle(workbook, HSSFColor.HSSFColorPredefined.ROSE.getIndex());
            HSSFCellStyle yel = makeXlsStyle(workbook, HSSFColor.HSSFColorPredefined.LIGHT_YELLOW.getIndex());
            HSSFCellStyle blu = makeXlsStyle(workbook, HSSFColor.HSSFColorPredefined.LIGHT_BLUE.getIndex());
            HSSFCellStyle gry = makeXlsStyle(workbook, HSSFColor.HSSFColorPredefined.GREY_25_PERCENT.getIndex());

            for (int i = 0; i < data.size(); i++) {
                HSSFRow row = sheet.createRow(i);
                List<String> rowData = data.get(i);
                for (int j = 0; j < rowData.size(); j++) {
                    HSSFCell cell = row.createCell(j);
                    String value = rowData.get(j);
                    cell.setCellValue(value != null ? value : "");
                    if (i == 0) cell.setCellStyle(hdr);
                    else if (j == 0) {
                        if      ("HIGH".equals(value))   cell.setCellStyle(red);
                        else if ("MEDIUM".equals(value)) cell.setCellStyle(yel);
                        else if ("LOW".equals(value))    cell.setCellStyle(blu);
                        else if ("INFO".equals(value))   cell.setCellStyle(gry);
                    }
                }
            }
            if (!data.isEmpty())
                for (int i = 0; i < data.get(0).size(); i++) sheet.autoSizeColumn(i);
            try (FileOutputStream fos = new FileOutputStream(filePath)) {
                workbook.write(fos);
            }
        } catch (Exception e) {
            System.out.println("Error writing flag report: " + e.getMessage());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // STYLE HELPERS
    // ─────────────────────────────────────────────────────────────────────────

    private XSSFCellStyle makeXlsxStyle(XSSFWorkbook wb, String hexColor) {
        XSSFCellStyle style = wb.createCellStyle();
        style.setFillForegroundColor(new XSSFColor(hexToBytes(hexColor), null));
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return style;
    }

    private XSSFCellStyle makeXlsxHeaderStyle(XSSFWorkbook wb) {
        XSSFCellStyle style = wb.createCellStyle();
        style.setFillForegroundColor(new XSSFColor(hexToBytes("1F4E79"), null));
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        XSSFFont font = wb.createFont();
        font.setColor(new XSSFColor(hexToBytes("FFFFFF"), null));
        font.setBold(true);
        style.setFont(font);
        return style;
    }

    private HSSFCellStyle makeXlsStyle(HSSFWorkbook wb, short colorIndex) {
        HSSFCellStyle style = wb.createCellStyle();
        style.setFillForegroundColor(colorIndex);
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return style;
    }

    private HSSFCellStyle makeXlsHeaderStyle(HSSFWorkbook wb) {
        HSSFCellStyle style = wb.createCellStyle();
        style.setFillForegroundColor(HSSFColor.HSSFColorPredefined.DARK_BLUE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        HSSFFont font = wb.createFont();
        font.setColor(HSSFColor.HSSFColorPredefined.WHITE.getIndex());
        font.setBold(true);
        style.setFont(font);
        return style;
    }

    private XSSFCellStyle getTypeStyle(XSSFWorkbook wb, String type,
                                       Map<String, XSSFCellStyle> cache) {
        return cache.computeIfAbsent(type, t -> {
            XSSFCellStyle style = wb.createCellStyle();
            String fmt = EXCEL_FORMATS.get(t);
            if (fmt != null) {
                DataFormat df = wb.createDataFormat();
                style.setDataFormat(df.getFormat(fmt));
            }
            return style;
        });
    }

    private XSSFCellStyle getFlagColorStyleXlsx(XSSFWorkbook wb, String flagType,
                                                XSSFCellStyle red,
                                                XSSFCellStyle yellow,
                                                XSSFCellStyle blue) {
        if (flagType == null) return null;
        if (flagType.startsWith("INVALID"))    return red;
        if (flagType.startsWith("SUSPICIOUS")) return yellow;
        return blue;
    }

    private HSSFCellStyle getFlagColorStyleXls(String flagType,
                                               HSSFCellStyle red,
                                               HSSFCellStyle yellow,
                                               HSSFCellStyle blue) {
        if (flagType == null) return null;
        if (flagType.startsWith("INVALID"))    return red;
        if (flagType.startsWith("SUSPICIOUS")) return yellow;
        return blue;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PARSE HELPERS
    // ─────────────────────────────────────────────────────────────────────────

    private boolean isFlaggedContent(String value) {
        if (value == null) return false;
        for (String s : FLAG_STARTS) if (value.startsWith(s)) return true;
        return false;
    }

    private String detectFlagType(String value) {
        if (value == null) return null;
        if (value.startsWith("[INVALID_"))    return "INVALID";
        if (value.startsWith("[SUSPICIOUS_")) return "SUSPICIOUS";
        return "WARNING";
    }

    private double parseDouble(String value) {
        String cleaned = value.replaceAll("[,$€£¥₹'\\s]","")
                .replaceAll("^\\((.*)\\)$", "-$1");
        if (cleaned.matches(".*\\d,\\d{1,2}$"))
            cleaned = cleaned.replace(",",".");
        else
            cleaned = cleaned.replace(",","");
        return Double.parseDouble(cleaned);
    }

    private LocalDate parseDate(String value) {
        String[] patterns = {
                "yyyy-MM-dd","MM/dd/yyyy","dd/MM/yyyy","yyyy/MM/dd",
                "M/d/yyyy","d/M/yyyy","MMMM d yyyy","MMM d yyyy",
                "MM-dd-yyyy","dd-MM-yyyy"
        };
        for (String p : patterns) {
            try { return LocalDate.parse(value.trim(),
                    DateTimeFormatter.ofPattern(p, Locale.ENGLISH)); }
            catch (Exception ignored) {}
        }
        return null;
    }

    private LocalTime parseTime(String value) {
        String[] patterns = {
                "HH:mm:ss","HH:mm","H:mm:ss","H:mm",
                "hh:mm:ss a","hh:mm a","h:mm:ss a","h:mm a","h:mma"
        };
        for (String p : patterns) {
            try { return LocalTime.parse(value.trim(),
                    new DateTimeFormatterBuilder().parseCaseInsensitive()
                            .appendPattern(p).toFormatter(Locale.ENGLISH)); }
            catch (Exception ignored) {}
        }
        return null;
    }

    private LocalDateTime parseDateTime(String value) {
        String[] patterns = {
                "yyyy-MM-dd HH:mm:ss","yyyy-MM-dd HH:mm",
                "yyyy-MM-dd'T'HH:mm:ss","MM/dd/yyyy HH:mm","dd/MM/yyyy HH:mm"
        };
        for (String p : patterns) {
            try { return LocalDateTime.parse(value.trim(),
                    DateTimeFormatter.ofPattern(p, Locale.ENGLISH)); }
            catch (Exception ignored) {}
        }
        return null;
    }

    private Boolean parseBoolean(String value) {
        String v = value.trim().toLowerCase();
        if (v.equals("true") || v.equals("1") || v.equals("yes")) return Boolean.TRUE;
        if (v.equals("false")|| v.equals("0") || v.equals("no"))  return Boolean.FALSE;
        return null;
    }

    private byte[] hexToBytes(String hex) {
        hex = hex.replace("#","");
        return new byte[]{
                (byte)Integer.parseInt(hex.substring(0,2),16),
                (byte)Integer.parseInt(hex.substring(2,4),16),
                (byte)Integer.parseInt(hex.substring(4,6),16)
        };
    }
}