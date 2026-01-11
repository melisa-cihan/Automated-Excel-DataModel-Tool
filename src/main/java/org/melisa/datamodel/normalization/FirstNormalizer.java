package org.melisa.datamodel.normalization;

import java.util.ArrayList;
import java.util.List;
import java.util.Collections;
import java.util.Map;
import java.util.LinkedHashMap; // Explicitly used for row maps
import java.util.regex.Pattern;

import org.melisa.datamodel.normalization.heuristics.HeuristicRule;
import org.melisa.datamodel.normalization.heuristics.QuantityItemHeuristic;
import org.melisa.datamodel.normalization.heuristics.ValueUnitHeuristic;
import org.melisa.datamodel.normalization.heuristics.ParentheticalAliasHeuristic;
import org.melisa.datamodel.normalization.heuristics.CurrencyHeuristic;


public class FirstNormalizer {

    private static final Pattern ROW_SPLITTING_DELIMITERS = Pattern.compile(";|\\||\\n|\\r|,(?!\\d)");

    /**
     * This list defines the order in which column-splitting heuristics are applied.
     * The order can be crucial as some heuristics might take precedence over others.
     * For instance, a more specific pattern should typically come before a more general one.
     */
    private static final List<HeuristicRule> COLUMN_SPLITTING_RULES = List.of(
            new CurrencyHeuristic(),          // Next, specific for currency values
            new ValueUnitHeuristic(),         // E.g., "50 kg" - more general number-unit
            new QuantityItemHeuristic(),      // E.g., "2 books" - very specific pattern
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


        List<Map<String, Object>> afterRowSplitting = applyRowSplittingHeuristics(rawData);


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

            Map<String, List<String>> multiValueColumns = new LinkedHashMap<>();


            List<String> singleValueColumnNames = new ArrayList<>();


            for (Map.Entry<String, Object> entry : originalRow.entrySet()) {
                String originalColumnName = entry.getKey();
                Object cellValue = entry.getValue();

                if (cellValue instanceof String stringValue) {
                    String trimmedStringValue = stringValue.trim();


                    if (ROW_SPLITTING_DELIMITERS.matcher(trimmedStringValue).find()) {
                        String[] parts = trimmedStringValue.split(ROW_SPLITTING_DELIMITERS.pattern());
                        List<String> trimmedParts = new ArrayList<>();
                        for (String part : parts) {
                            trimmedParts.add(part.trim());
                        }
                        multiValueColumns.put(originalColumnName, trimmedParts);
                    } else {

                        singleValueColumnNames.add(originalColumnName);
                    }
                } else {

                    singleValueColumnNames.add(originalColumnName);
                }
            }


            if (multiValueColumns.isEmpty()) {
                outputData.add(originalRow);
            } else {

                List<Map<String, Object>> generatedRows = new ArrayList<>();

                generateCartesianProductRecursive(
                        multiValueColumns,
                        new LinkedHashMap<>(),
                        new ArrayList<>(multiValueColumns.keySet()),
                        0,
                        generatedRows
                );


                for (Map<String, Object> generatedRow : generatedRows) {
                    Map<String, Object> finalNewRow = new LinkedHashMap<>();

                    for (String colName : originalRow.keySet()) {
                        if (singleValueColumnNames.contains(colName)) {
                            finalNewRow.put(colName, originalRow.get(colName));
                        }
                    }

                    finalNewRow.putAll(generatedRow);
                    outputData.add(finalNewRow);
                }
            }
        }
        return outputData;
    }

    /**
     * Recursive helper method to generate the Cartesian product of multivalued columns.
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


        if (keyIndex == columnKeys.size()) {
            resultRows.add(new LinkedHashMap<>(currentProductRow));
            return;
        }


        String currentColumnName = columnKeys.get(keyIndex);
        List<String> values = multiValueColumns.getOrDefault(currentColumnName, Collections.emptyList());


        for (String value : values) {
            currentProductRow.put(currentColumnName, value);

            generateCartesianProductRecursive(
                    multiValueColumns,
                    currentProductRow,
                    columnKeys,
                    keyIndex + 1,
                    resultRows
            );

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

            Map<String, Object> newRow = new LinkedHashMap<>();


            for (Map.Entry<String, Object> entry : originalRow.entrySet()) {
                String originalColumnName = entry.getKey();
                Object cellValue = entry.getValue();

                boolean heuristicApplied = false;


                if (cellValue instanceof String) {

                    for (HeuristicRule rule : COLUMN_SPLITTING_RULES) {

                        if (rule.apply(originalColumnName, cellValue, newRow)) {
                            heuristicApplied = true;
                            break;
                        }
                    }
                }

                if (!heuristicApplied) {
                    newRow.put(originalColumnName, cellValue);
                }

            }
            outputData.add(newRow);
        }
        return outputData;
    }
}