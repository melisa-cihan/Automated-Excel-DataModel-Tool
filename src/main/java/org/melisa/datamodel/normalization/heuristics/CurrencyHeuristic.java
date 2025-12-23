package org.melisa.datamodel.normalization.heuristics;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Heuristic rule to split a string like "50 €" or "$60" into Amount and Currency columns.
 * It handles currency symbols at the start or the end of the numerical value, with optional spaces.
 */
public class CurrencyHeuristic implements HeuristicRule {
    // Matches: [Optional Prefix Symbol][Optional Space][Number][Optional Space][Optional Suffix Symbol]
    // Group 1: Optional prefix symbol(s) (e.g., $)
    // Group 2: The numerical part (e.g., 60 or 50.00)
    // Group 4: Optional suffix symbol(s) (e.g., €)
    private static final Pattern CURRENCY_PATTERN =
            Pattern.compile("^([$€£¥₹₩¢]+)?\\s*(-?\\d+(\\.\\d+)?)\\s*([$€£¥₹₩¢]+)?$");

    @Override
    public boolean apply(String originalColumnName, Object cellValue, Map<String, Object> newRow) {
        if (!(cellValue instanceof String stringValue)) {
            return false;
        }

        String trimmedStringValue = stringValue.trim();
        Matcher matcher = CURRENCY_PATTERN.matcher(trimmedStringValue);

        if (matcher.find()) {
            String startSymbol = matcher.group(1); // Group 1 is the prefix symbol
            String numericalPart = matcher.group(2); // Group 2 is the numerical amount
            String endSymbol = matcher.group(4);     // Group 4 is the suffix symbol

            // Determine the currency symbol. It must be present for this heuristic to apply
            // (based on your current logic structure).
            String currencySymbol;
            if (startSymbol != null && !startSymbol.isEmpty()) {
                currencySymbol = startSymbol;
            } else if (endSymbol != null && !endSymbol.isEmpty()) {
                currencySymbol = endSymbol;
            } else {
                // If neither prefix nor suffix symbol is found (e.g., "150"), return false.
                return false;
            }

            try {
                // Use the determined numericalPart and currencySymbol
                newRow.put(originalColumnName + "_Amount", Double.parseDouble(numericalPart));
                newRow.put(originalColumnName + "_Currency", currencySymbol.trim());
                return true;
            } catch (NumberFormatException e) {
                // This handles cases where the "numericalPart" (Group 2) isn't a valid double.
                return false;
            }
        }
        return false;
    }
}
