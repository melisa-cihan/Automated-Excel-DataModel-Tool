package org.melisa.datamodel.normalization.heuristics;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Heuristic rule to split a string like "50 kg", "20.5°C", "5 in.", or "100
 * USD" into Value and Unit columns.
 * Implements the org.melisa.datamodel.normalization.heuristics.HeuristicRule
 * interface.
 * This heuristic is designed to be a general fallback for physical measurements
 * and currencies.
 */
public class ValueUnitHeuristic implements HeuristicRule {

    private static final Pattern VALUE_UNIT_PATTERN = Pattern.compile("^(-?\\d+([.,]\\d+)?)\\s*([a-zA-Z%°\\.]{1,3})$");

    /**
     * Splits a string like "50 kg" or "20.5°C" into numeric value and unit
     * components.
     *
     * @param originalColumnName The original name of the column.
     * @param cellValue          The original cell value to process.
     * @param newRow             The map where new columns ("_Value" and "_Unit")
     *                           will be stored.
     * @return true if the value was successfully split, false otherwise.
     */
    @Override
    public boolean apply(String originalColumnName, Object cellValue, Map<String, Object> newRow) {
        if (!(cellValue instanceof String stringValue)) {
            return false;
        }

        String trimmedStringValue = stringValue.trim();

        if (originalColumnName.toLowerCase().contains("_value") || originalColumnName.toLowerCase().contains("_unit")
                || originalColumnName.toLowerCase().contains("_quantity")
                || originalColumnName.toLowerCase().contains("_item")) {
            return false;
        }

        Matcher valueUnitMatcher = VALUE_UNIT_PATTERN.matcher(trimmedStringValue);

        if (valueUnitMatcher.find()) {
            try {

                String numberString = valueUnitMatcher.group(1).replace(",", ".");
                newRow.put(originalColumnName + "_Value", Double.parseDouble(numberString));

                newRow.put(originalColumnName + "_Unit", valueUnitMatcher.group(3).trim());
                return true;
            } catch (NumberFormatException e) {
                return false;
            }
        }
        return false;
    }
}
