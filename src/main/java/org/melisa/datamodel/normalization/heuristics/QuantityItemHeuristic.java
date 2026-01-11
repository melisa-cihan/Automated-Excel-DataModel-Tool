package org.melisa.datamodel.normalization.heuristics;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Heuristic rule to split a string like "2 books" into Quantity and Item columns.
 * Implements the org.melisa.datamodel.normalization.heuristics.HeuristicRule interface.
 */
public class QuantityItemHeuristic implements HeuristicRule {
    private static final Pattern NUMERIC_PREFIX_PATTERN = Pattern.compile("^(\\d+)\\s+(.*)$", Pattern.CASE_INSENSITIVE);

    @Override
    public boolean apply(String originalColumnName, Object cellValue, Map<String, Object> newRow) {

        if (!(cellValue instanceof String stringValue)) {
            return false;
        }

        String trimmedStringValue = stringValue.trim();
        Matcher numericMatcher = NUMERIC_PREFIX_PATTERN.matcher(trimmedStringValue);

        if (numericMatcher.find()) {
            try {

                newRow.put(originalColumnName + "_Quantity", Integer.parseInt(numericMatcher.group(1)));
                newRow.put(originalColumnName + "_Item", numericMatcher.group(2).trim());
                return true;
            } catch (NumberFormatException e) {

                return false;
            }
        }

        return false;
    }
}
