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

        System.out.println("Please enter the desired SQL table name base (e.g., 'Order').");
        System.out.println("If left blank, a default name like 'EXCEL_DATA' will be used:");
        String tableNameInput = scanner.nextLine();
        String tableNameBase = tableNameInput.trim().isEmpty() ? "EXCEL_DATA" : tableNameInput.trim();

        try (InputStream fileInputStream = new FileInputStream(filePath)) {
            // --- Step 1: Read Excel Data ---
            System.out.println("\n--- Step 1: Reading Excel data ---");
            List<Map<String, Object>> excelData = ReadExcelFile.readExcelData(fileInputStream);
            System.out.println("Excel data read successfully. Number of rows detected: " + excelData.size());


            // --- Step 2: Normalizing data to First Normal Form (1NF) ---
            // This is necessary because the CandidateKeyIdentifier/SecondNormalizer relies on 1NF data.
            System.out.println("\n--- Step 2: Normalizing data to First Normal Form (1NF) ---");
            List<Map<String, Object>> normalized1NFData = Normalizer.normalizeTo1NF(excelData);
            System.out.println("1NF Normalization complete. Number of normalized rows: " + normalized1NFData.size());


            // --- Step 3: Normalizing data to Second Normal Form (2NF) ---
            System.out.println("\n--- Step 3: Decomposing data to Second Normal Form (2NF) ---");
            SecondNormalizer secondNormalizer = new SecondNormalizer();
            // This map holds the original relation split into potentially multiple 2NF relations
            Map<String, List<Map<String, Object>>> normalized2NFRelations =
                    secondNormalizer.normalizeTo2NF(normalized1NFData);

            System.out.println("2NF Decomposition complete. Generated " + normalized2NFRelations.size() + " new relation(s).");


            // --- Step 4: Display Results and Generate SQL script ---
            System.out.println("\n--- Step 4: Displaying 2NF Relations and Generating SQL ---");

            for (Map.Entry<String, List<Map<String, Object>>> entry : normalized2NFRelations.entrySet()) {
                String relationName = tableNameBase + "_" + entry.getKey();
                List<Map<String, Object>> relationData = entry.getValue();

                System.out.println("\n== Relation Name: " + relationName + " ==");

                // Display the data preview for the new relation
                for (Map<String, Object> row : relationData) {
                    System.out.println(row);
                }

                // Generate SQL for the new relation
                String sqlScript = SqlGenerator.generateSqlScript(relationData, relationName);

                System.out.println("\n--- START SQL SCRIPT for " + relationName + " ---\n");
                System.out.println(sqlScript);
                System.out.println("\n--- END SQL SCRIPT for " + relationName + " ---\n");
            }


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
