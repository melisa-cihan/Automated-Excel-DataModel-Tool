package org.melisa.datamodel.normalization.heuristics;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Heuristic rule to split a string like "50 €" or "$60" into Amount and Currency columns.
 * It handles currency symbols at the start or the end of the numerical value, with optional spaces.
 */
public class CurrencyHeuristic implements HeuristicRule {

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
            String startSymbol = matcher.group(1);
            String numericalPart = matcher.group(2);
            String endSymbol = matcher.group(4);


            String currencySymbol;
            if (startSymbol != null && !startSymbol.isEmpty()) {
                currencySymbol = startSymbol;
            } else if (endSymbol != null && !endSymbol.isEmpty()) {
                currencySymbol = endSymbol;
            } else {

                return false;
            }

            try {

                newRow.put(originalColumnName + "_Amount", Double.parseDouble(numericalPart));
                newRow.put(originalColumnName + "_Currency", currencySymbol.trim());
                return true;
            } catch (NumberFormatException e) {

                return false;
            }
        }
        return false;
    }
}
