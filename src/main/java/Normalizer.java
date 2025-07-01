import java.util.ArrayList;
import java.util.List;
import java.util.Collections;
import java.util.Map;
import java.util.LinkedHashMap; // Explicitly used for row maps
import java.util.regex.Pattern;

import normalizer.heuristics.HeuristicRule;
import normalizer.heuristics.QuantityItemHeuristic;
import normalizer.heuristics.ValueUnitHeuristic;
import normalizer.heuristics.ParentheticalAliasHeuristic;
import normalizer.heuristics.CurrencyHeuristic;


public class Normalizer {

    // --- Heuristic Definitions (only for row splitting, column splitting now uses rules)---

    // Heuristic 1: Comma-separated values (leads to ROW SPLITTING)
    // Example: "green,yellow" -> becomes two rows
    private static final Pattern COMMA_SEPARATED_PATTERN = Pattern.compile(",");

    /**
     * This list defines the order in which column-splitting heuristics are applied.
     * The order can be crucial as some heuristics might take precedence over others.
     * For instance, a more specific pattern should typically come before a more general one.
     */
    private static final List<HeuristicRule> COLUMN_SPLITTING_RULES = List.of(
            new CurrencyHeuristic(),          // Next, specific for currency values
            new QuantityItemHeuristic(),      // E.g., "2 books" - very specific pattern
            new ValueUnitHeuristic(),         // E.g., "50 kg" - more general number-unit
            new ParentheticalAliasHeuristic() // E.g., "Name (Alias)"
            // Add new column-splitting heuristics here,
            //the order of the rules plays a role
    );

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
        // This pass will now generate a Cartesian product for multiple multi-valued columns.
        List<Map<String, Object>> afterRowSplitting = applyRowSplittingHeuristics(rawData);

        // Pass 2: Apply column-splitting heuristics (e.g., quantity-item, parenthetical alias)
        // This pass modifies columns within existing rows.
        return applyColumnSplittingHeuristics(afterRowSplitting);
    }

    /**
     * Applies heuristics that lead to splitting a single row into multiple rows,
     * generating a Cartesian product if multiple columns in the same row need splitting.
     *
     * @param inputData The list of rows to process.
     * @return A new list of rows, potentially much larger than inputData if splits occurred.
     */
    private static List<Map<String, Object>> applyRowSplittingHeuristics(List<Map<String, Object>> inputData) {
        List<Map<String, Object>> outputData = new ArrayList<>();

        for (Map<String, Object> originalRow : inputData) {
            // Map to store columns that contain multiple values and their split parts.
            // Keys are column names, values are lists of split parts.
            Map<String, List<String>> multiValueColumns = new LinkedHashMap<>();

            // List to store names of columns that are *not* split for row expansion.
            List<String> singleValueColumnNames = new ArrayList<>();

            // First pass: Identify all columns in the current row that need row splitting
            for (Map.Entry<String, Object> entry : originalRow.entrySet()) {
                String originalColumnName = entry.getKey();
                Object cellValue = entry.getValue();

                if (cellValue instanceof String stringValue) {
                    String trimmedStringValue = stringValue.trim();

                    // Check for comma-separated pattern (or other row-splitting patterns)
                    if (COMMA_SEPARATED_PATTERN.matcher(trimmedStringValue).find()) {
                        String[] parts = trimmedStringValue.split(COMMA_SEPARATED_PATTERN.pattern());
                        List<String> trimmedParts = new ArrayList<>();
                        for (String part : parts) {
                            trimmedParts.add(part.trim());
                        }
                        multiValueColumns.put(originalColumnName, trimmedParts);
                    } else {
                        // This column is not a multi-value string for row splitting
                        singleValueColumnNames.add(originalColumnName);
                    }
                } else {
                    // Non-string values also don't trigger row splitting
                    singleValueColumnNames.add(originalColumnName);
                }
            }

            // If no columns needed row splitting, add the original row as is
            if (multiValueColumns.isEmpty()) {
                outputData.add(originalRow);
            } else {
                // If there are multi-value columns, generate their Cartesian product
                List<Map<String, Object>> generatedRows = new ArrayList<>();
                // Call the recursive helper to build the Cartesian product
                generateCartesianProductRecursive(
                        multiValueColumns,                  // Columns that need to be expanded
                        new LinkedHashMap<>(),              // Current partial row being built (starts empty)
                        new ArrayList<>(multiValueColumns.keySet()), // Keys to iterate through
                        0,                                  // Current key index
                        generatedRows                       // List to add the final product rows to
                );

                // For each generated row from the Cartesian product, populate non-split columns
                for (Map<String, Object> generatedRow : generatedRows) {
                    Map<String, Object> finalNewRow = new LinkedHashMap<>();
                    // First, add the non-split columns in their original order
                    for (String colName : originalRow.keySet()) { // Iterate originalRow keys to preserve order
                        if (singleValueColumnNames.contains(colName)) {
                            finalNewRow.put(colName, originalRow.get(colName));
                        }
                    }
                    // Then, add the values from the Cartesian product for the split columns
                    finalNewRow.putAll(generatedRow);
                    outputData.add(finalNewRow);
                }
            }
        }
        return outputData;
    }

    /**
     * Recursive helper method to generate the Cartesian product of multi-valued columns.
     *
     * @param multiValueColumns The map of column names to lists of their split values.
     * @param currentProductRow The partial row being built during recursion.
     * @param columnKeys A list of column names (keys) from multiValueColumns to iterate through.
     * @param keyIndex The current index in columnKeys being processed.
     * @param resultRows The list to which the final Cartesian product rows will be added.
     */
    private static void generateCartesianProductRecursive(
            Map<String, List<String>> multiValueColumns,
            Map<String, Object> currentProductRow,
            List<String> columnKeys,
            int keyIndex,
            List<Map<String, Object>> resultRows) {

        // Base case: If we have processed all multi-value columns, a complete row is formed.
        if (keyIndex == columnKeys.size()) {
            resultRows.add(new LinkedHashMap<>(currentProductRow)); // Add a copy of the completed row
            return;
        }

        // Recursive step: Get the current column to process
        String currentColumnName = columnKeys.get(keyIndex);
        List<String> values = multiValueColumns.getOrDefault(currentColumnName, Collections.emptyList());

        // Iterate through each value in the current column's list
        for (String value : values) {
            currentProductRow.put(currentColumnName, value); // Add the current value to the row
            // Recurse for the next column
            generateCartesianProductRecursive(
                    multiValueColumns,
                    currentProductRow,
                    columnKeys,
                    keyIndex + 1, // Move to the next column
                    resultRows
            );
            // Backtrack: Remove the current column's value for the next iteration (important for LinkedHashMap)
            // This ensures currentProductRow is clean for sibling recursive calls.
            currentProductRow.remove(currentColumnName);
        }
    }


    /**
     * Applies heuristics that lead to splitting values within a column into new columns.
     * This pass does not change the number of rows and uses a list of HeuristicRule objects for extensibility and cleaner code.
     *
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

                boolean heuristicApplied = false; // Flag to track if any heuristic successfully processed the value

                // Only apply column-splitting heuristics if the value is a String
                if (cellValue instanceof String) { // No need for 'stringValue' variable here, pass 'cellValue' directly to apply
                    // Iterate through the predefined list of HeuristicRules.
                    // The order of rules in the list defines their application priority.
                    for (HeuristicRule rule : COLUMN_SPLITTING_RULES) {
                        // Attempt to apply the current rule.
                        // If 'rule.apply()' returns true, it means the rule processed the value
                        // and added its results to 'newRow'. No other rule needs to be tried for this cell.
                        if (rule.apply(originalColumnName, cellValue, newRow)) {
                            heuristicApplied = true;
                            break; // Stop trying rules for this cell, move to the next original column.
                        }
                    }
                }
                // If no heuristic was applied (either because it wasn't a String, or no rule matched),
                // then simply add the original column and its value to the new row as is.
                if (!heuristicApplied) {
                    newRow.put(originalColumnName, cellValue);
                }

            }
            outputData.add(newRow);
        }
        return outputData;
    }
}