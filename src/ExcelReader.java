import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import java.io.*;
import java.util.*;

public class ExcelReader {

    public List<List<String>> read(String filePath) {
        List<List<String>> data = new ArrayList<>();

        try (FileInputStream fis = new FileInputStream(filePath)) {

            Workbook workbook;
            if (filePath.toLowerCase().endsWith(".xlsx")) {
                workbook = new XSSFWorkbook(fis);
            } else {
                workbook = new HSSFWorkbook(fis);
            }

            Sheet sheet = workbook.getSheetAt(0);

            for (Row row : sheet) {
                List<String> rowData = new ArrayList<>();
                for (Cell cell : row) {
                    switch (cell.getCellType()) {
                        case STRING:
                            rowData.add(cell.getStringCellValue());
                            break;
                        case NUMERIC:
                            if (DateUtil.isCellDateFormatted(cell)) {
                                rowData.add(cell.getLocalDateTimeCellValue().toString());
                            } else {
                                double val = cell.getNumericCellValue();
                                if (val == Math.floor(val)) {
                                    rowData.add(String.valueOf((long) val));
                                } else {
                                    rowData.add(String.valueOf(val));
                                }
                            }
                            break;
                        case BOOLEAN:
                            rowData.add(String.valueOf(cell.getBooleanCellValue()));
                            break;
                        case BLANK:
                            rowData.add("");
                            break;
                        default:
                            rowData.add("");
                    }
                }
                data.add(rowData);
            }

            workbook.close();

        } catch (Exception e) {
            System.out.println("Error reading Excel file: " + e.getMessage());
        }

        return data;
    }
}