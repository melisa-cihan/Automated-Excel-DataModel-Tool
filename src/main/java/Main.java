import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);


        System.out.println("Please enter the full path to your Excel file (e.g., C:\\data\\mydata.xlsx or /home/user/data.xls):");
        String filePath = scanner.nextLine();

        System.out.println("Please enter the desired SQL table name (e.g., 'MyNormalizedData').");
        System.out.println("If left blank, a default name like 'EXCEL_DATA' will be used:");
        String tableNameInput = scanner.nextLine();
        String tableName = tableNameInput.trim().isEmpty() ? "EXCEL_DATA" : tableNameInput.trim();

        try (InputStream fileInputStream = new FileInputStream(filePath)) {
            // --- Step 1: Read Excel Data ---
            System.out.println("\n--- Step 1: Reading Excel data ---");
            List<Map<String, Object>> excelData = ReadExcelFile.readExcelData(fileInputStream);
            System.out.println("Excel data read successfully. Number of rows detected: " + excelData.size());


            // --- Step 2: Normalizing data to First Normal Form (1NF) ---
            System.out.println("\n--- Step 2: Normalizing data to First Normal Form (1NF) ---");
            List<Map<String, Object>> normalizedData = Normalizer.normalizeTo1NF(excelData);
            System.out.println("Normalization complete. Number of normalized rows: " + normalizedData.size());

            // Display the entire normalized data preview
            System.out.println("\nFull Preview of Normalized Data:");
            for (Map<String, Object> row : normalizedData) {
                System.out.println(row);
            }


            // --- Step 3: Generate SQL script ---
            System.out.println("\n--- Step 3: Generating SQL script ---");
            String sqlScript = SqlGenerator.generateSqlScript(normalizedData, tableName);
            System.out.println("\nSQL script generated successfully:\n");

            System.out.println("\n--- START SQL SCRIPT ---\n");
            System.out.println(sqlScript);
            System.out.println("\n--- END SQL SCRIPT ---\n");

        } catch (IOException e) {
            System.err.println("Error reading the Excel file. Please check the path and file permissions: " + e.getMessage());
        } catch (IllegalArgumentException e) {
            System.err.println("Error with Excel file format or content: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("An unexpected error occurred during processing: " + e.getMessage());
            e.printStackTrace(); // Print full stack trace for detailed debugging
        } finally {
            scanner.close(); // Ensure the main scanner is closed
        }
    }
}