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
            // NOTE: Assumes ReadExcelFile class exists with a static readExcelData method
            List<Map<String, Object>> excelData = ReadExcelFile.readExcelData(fileInputStream);
            System.out.println("Excel data read successfully. Number of rows detected: " + excelData.size());


            // --- Step 2: Normalizing data to First Normal Form (1NF) ---
            System.out.println("\n--- Step 2: Normalizing data to First Normal Form (1NF) ---");
            // NOTE: Assumes Normalizer class exists with a static normalizeTo1NF method
            List<Map<String, Object>> normalized1NFData = Normalizer.normalizeTo1NF(excelData);
            System.out.println("1NF Normalization complete. Number of normalized rows: " + normalized1NFData.size());


            // --- Step 3: Normalizing data to Second Normal Form (2NF) ---
            System.out.println("\n--- Step 3: Decomposing data to Second Normal Form (2NF) ---");
            SecondNormalizer secondNormalizer = new SecondNormalizer();

            // *** ADAPTATION 1: Capture the List of DecomposedRelation objects ***
            List<DecomposedRelation> decomposedRelations =
                    secondNormalizer.normalizeTo2NF(normalized1NFData);

            System.out.println("2NF Decomposition complete. Generated " + decomposedRelations.size() + " new relation(s).");


            // --- Step 4: Display Results and Generate SQL script ---
            System.out.println("\n--- Step 4: Displaying 2NF Relations and Generating SQL ---");

            for (DecomposedRelation relation : decomposedRelations) {
                // The final SQL table name combines the user base name and the relation's internal name
                String relationName = tableNameBase + "_" + relation.name();

                System.out.println("\n== Relation Name: " + relationName + " ==");
                System.out.println("   Primary Keys: " + relation.primaryKeys());
                System.out.println("   Foreign Keys: " + relation.foreignKeys());

                // Display the data preview for the new relation
                // NOTE: Using relation.data() and relation.primaryKeys()/foreignKeys() from the record
                for (Map<String, Object> row : relation.data()) {
                    System.out.println(row);
                }

                // *** ADAPTATION 2: Pass key metadata to the SqlGenerator's updated method ***
                String sqlScript = SqlGenerator.generateSqlScript(
                        relation.data(),
                        relationName,
                        relation.primaryKeys(),
                        relation.foreignKeys()
                );

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
