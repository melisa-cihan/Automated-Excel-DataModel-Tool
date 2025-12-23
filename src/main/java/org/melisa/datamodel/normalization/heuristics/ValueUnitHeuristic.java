package org.melisa.datamodel.normalization.heuristics;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Heuristic rule to split a string like "50 kg", "20.5°C", "5 in.", or "100 USD" into Value and Unit columns.
 * Implements the org.melisa.datamodel.normalization.heuristics.HeuristicRule interface.
 * This heuristic is designed to be a general fallback for physical measurements and currencies.
 */
public class ValueUnitHeuristic implements HeuristicRule {
    // MODIFIED: The unit group now includes the degree symbol (°), period (.), and other non-alphabetic symbols.
    // Pattern: [Number] [Optional Space] [Unit (letters, %, °, .)]
    private static final Pattern VALUE_UNIT_PATTERN = Pattern.compile("^(-?\\d+([.,]\\d+)?)\\s*([a-zA-Z%°\\.]{1,3})$");

    @Override
    public boolean apply(String originalColumnName, Object cellValue, Map<String, Object> newRow) {
        if (!(cellValue instanceof String stringValue)) {
            return false;
        }

        String trimmedStringValue = stringValue.trim();

        // Safety check to ensure we don't accidentally run this on an already processed column
        if (originalColumnName.toLowerCase().contains("_value") || originalColumnName.toLowerCase().contains("_unit")
                || originalColumnName.toLowerCase().contains("_quantity") || originalColumnName.toLowerCase().contains("_item")) {
            return false;
        }

        Matcher valueUnitMatcher = VALUE_UNIT_PATTERN.matcher(trimmedStringValue);

        if (valueUnitMatcher.find()) {
            try {
                // Group 1 is the number (e.g. "101,1"). Replace comma with dot for Java.
                String numberString = valueUnitMatcher.group(1).replace(",", ".");
                newRow.put(originalColumnName + "_Value", Double.parseDouble(numberString));

                // Group 3 is the unit
                newRow.put(originalColumnName + "_Unit", valueUnitMatcher.group(3).trim());
                return true;
            } catch (NumberFormatException e) {
                return false;
            }
        }
        return false;
    }
}
