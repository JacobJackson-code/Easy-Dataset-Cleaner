import com.opencsv.CSVWriter;
import java.io.FileWriter;
import java.util.*;

public class CSVwriter {

    public void write(String filePath, List<List<String>> data) {

        try (CSVWriter writer = new CSVWriter(new FileWriter(filePath))) {

            for (List<String> row : data) {
                writer.writeNext(row.toArray(new String[0]));
            }

        } catch (Exception e) {
            System.out.println("Error writing CSV: " + e.getMessage());
        }
    }
}