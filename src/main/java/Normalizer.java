import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap; // Explicitly used for row maps
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class Normalizer {

    // --- Heuristic Definitions ---

    // Heuristic 1: Comma-separated values (leads to ROW SPLITTING)
    // Example: "green,yellow" -> becomes two rows
    private static final Pattern COMMA_SEPARATED_PATTERN = Pattern.compile(",");

    // Heuristic 2: Quantity Item (leads to COLUMN SPLITTING)
    // Example: "2 books" -> "Product_Quantity": 2, "Product_Item": "books"
    private static final Pattern NUMERIC_PREFIX_PATTERN = Pattern.compile("^(\\d+)\\s+(.*)$", Pattern.CASE_INSENSITIVE);

    // Heuristic 3: Parenthetical Alias/Detail (leads to COLUMN SPLITTING)
    // Example: "IBA (ehemals BQL)" -> "Schülergruppe_Primary": "IBA", "Schülergruppe_Alias": "ehemals BQL"
    private static final Pattern PARENTHETICAL_ALIAS_PATTERN = Pattern.compile("^(.*?)\\s*\\((.*?)\\)$");


    /**
     * Normalizes a list of maps (representing Excel data) into the First Normal Form (1NF)
     * using automated heuristics. This robust version handles both row-splitting
     * and column-splitting heuristics in separate passes.
     *
     * @param rawData A list of maps, where each map represents a row of raw data from Excel.
     * @return A list of maps representing the data in 1NF.
     */
    public static List<Map<String, Object>> normalizeTo1NF(List<Map<String, Object>> rawData) {
        if (rawData == null || rawData.isEmpty()) {
            return new ArrayList<>();
        }

        // Pass 1: Apply row-splitting heuristics (e.g., comma-separated values)
        // This pass might increase the number of rows.
        List<Map<String, Object>> afterRowSplitting = applyRowSplittingHeuristics(rawData);

        // Pass 2: Apply column-splitting heuristics (e.g., quantity-item, parenthetical alias)
        // This pass modifies columns within existing rows.

        return applyColumnSplittingHeuristics(afterRowSplitting);
    }

    /**
     * Applies heuristics that lead to splitting a single row into multiple rows.
     *
     * @param inputData The list of rows to process.
     * @return A new list of rows, potentially larger than inputData if splits occurred.
     */
    private static List<Map<String, Object>> applyRowSplittingHeuristics(List<Map<String, Object>> inputData) {
        List<Map<String, Object>> outputData = new ArrayList<>();

        for (Map<String, Object> originalRow : inputData) {
            boolean rowWasSplit = false;

            // Iterate through each column to see if it triggers a row split
            for (Map.Entry<String, Object> entry : originalRow.entrySet()) {
                String originalColumnName = entry.getKey();
                Object cellValue = entry.getValue();

                if (cellValue instanceof String stringValue) {
                    String trimmedStringValue = stringValue.trim();

                    // Heuristic 1: Comma-separated values (ROW SPLITTING)
                    // Check if it *contains* a comma, not just if it *is* a comma
                    if (COMMA_SEPARATED_PATTERN.matcher(trimmedStringValue).find()) {
                        String[] parts = trimmedStringValue.split(COMMA_SEPARATED_PATTERN.pattern()); // Use the pattern for splitting
                        rowWasSplit = true; // Mark that this row will generate multiple new rows

                        for (String part : parts) {
                            // Create a new LinkedHashMap for each part to maintain order
                            Map<String, Object> newRow = new LinkedHashMap<>();
                            // Copy all columns from the original row EXCEPT the one being split
                            // Ensure order is maintained by iterating over originalRow's entry set
                            for (Map.Entry<String, Object> originalEntry : originalRow.entrySet()) {
                                if (!originalEntry.getKey().equals(originalColumnName)) {
                                    newRow.put(originalEntry.getKey(), originalEntry.getValue());
                                }
                            }
                            // Set the current part as the atomic value for the original column
                            newRow.put(originalColumnName, part.trim());
                            outputData.add(newRow); // Add the new row to the output list
                        }
                        // IMPORTANT: Once a column triggers a row split for this originalRow,
                        // we stop processing this originalRow's columns for row-splitting.
                        // The newly created rows will be processed in the next pass for column splitting.
                        break; // Exit the inner loop (column iteration) for this originalRow
                    }
                }
            }

            // If no column in the originalRow triggered a row split, add the originalRow as is
            // Note: originalRow is already a LinkedHashMap from ReadExcelFileGemini
            if (!rowWasSplit) {
                outputData.add(originalRow);
            }
        }
        return outputData;
    }

    /**
     * Applies heuristics that lead to splitting values within a column into new columns.
     * This pass does not change the number of rows.
     *
     * @param inputData The list of rows (already processed for row-splitting) to process.
     * @return A new list of rows with columns potentially expanded.
     */
    private static List<Map<String, Object>> applyColumnSplittingHeuristics(List<Map<String, Object>> inputData) {
        List<Map<String, Object>> outputData = new ArrayList<>();

        for (Map<String, Object> originalRow : inputData) {
            // Create a new LinkedHashMap for the transformed row to maintain column order
            Map<String, Object> newRow = new LinkedHashMap<>();

            // Iterate over the original row's entries to preserve order
            for (Map.Entry<String, Object> entry : originalRow.entrySet()) {
                String originalColumnName = entry.getKey();
                Object cellValue = entry.getValue();

                // Only apply these heuristics if the value is a String
                if (cellValue instanceof String stringValue) {
                    String trimmedStringValue = stringValue.trim();

                    // Heuristic 2: Quantity Item (COLUMN SPLITTING)
                    Matcher numericMatcher = NUMERIC_PREFIX_PATTERN.matcher(trimmedStringValue);
                    if (numericMatcher.find()) {
                        try {
                            String quantityColumnName = originalColumnName + "_Quantity";
                            String itemColumnName = originalColumnName + "_Item";
                            newRow.put(quantityColumnName, Integer.parseInt(numericMatcher.group(1)));
                            newRow.put(itemColumnName, numericMatcher.group(2).trim());
                        } catch (NumberFormatException e) {
                            // If parsing fails (e.g., "Five shirts"), treat as a regular string
                            newRow.put(originalColumnName, stringValue);
                        }
                    }
                    // Heuristic 3: Parenthetical Alias/Detail (COLUMN SPLITTING)
                    else {
                        Matcher aliasMatcher = PARENTHETICAL_ALIAS_PATTERN.matcher(trimmedStringValue);
                        if (aliasMatcher.find()) {
                            String primaryNameColumn = originalColumnName + "_Primary";
                            String aliasColumn = originalColumnName + "_Alias";
                            newRow.put(primaryNameColumn, aliasMatcher.group(1).trim());
                            newRow.put(aliasColumn, aliasMatcher.group(2).trim());
                        }
                        // If no column-splitting heuristic applies, keep the original string value
                        else {
                            newRow.put(originalColumnName, stringValue);
                        }
                    }
                } else {
                    // If the cell value is not a string (e.g., Integer, Double, Boolean, null), add it as is
                    newRow.put(originalColumnName, cellValue);
                }
            }
            outputData.add(newRow);
        }
        return outputData;
    }
}