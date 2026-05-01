import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReaderBuilder;
import java.io.FileReader;
import java.util.*;

public class CSVreader {

    public List<List<String>> read(String filePath) {
        // Try standard parse first
        List<List<String>> result = tryRead(filePath, false);

        // If failed, retry with lenient parser that ignores quote rules
        // This handles unterminated quoted fields from accidental line breaks
        if (result == null) {
            System.out.println("Standard CSV parse failed — retrying with lenient parser...");
            result = tryRead(filePath, true);
        }

        if (result == null) {
            System.out.println("Error reading file: could not parse CSV");
            return new ArrayList<>();
        }

        return result;
    }

    private List<List<String>> tryRead(String filePath, boolean lenient) {
        List<List<String>> data = new ArrayList<>();
        try (com.opencsv.CSVReader reader = new CSVReaderBuilder(new FileReader(filePath))
                .withCSVParser(new CSVParserBuilder()
                        .withIgnoreQuotations(lenient)
                        .build())
                .build()) {

            String[] values;
            while ((values = reader.readNext()) != null) {
                List<String> row = new ArrayList<>(Arrays.asList(values));
                data.add(row);
            }
            return data;

        } catch (Exception e) {
            if (!lenient) return null; // signal to retry
            System.out.println("Error reading file: " + e.getMessage());
            return null;
        }
    }
}