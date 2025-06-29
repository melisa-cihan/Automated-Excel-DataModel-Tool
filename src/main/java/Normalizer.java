import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap; // Explicitly used for row maps
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import normalizer.heuristics.HeuristicRule;
import normalizer.heuristics.QuantityItemHeuristic;
import normalizer.heuristics.ValueUnitHeuristic;
import normalizer.heuristics.ParentheticalAliasHeuristic;


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
            new QuantityItemHeuristic(),      // E.g., "2 books" - very specific pattern
            new ValueUnitHeuristic(),         // E.g., "50 kg" - more general number-unit
            new ParentheticalAliasHeuristic() // E.g., "Name (Alias)"
            // Add new column-splitting heuristics here
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