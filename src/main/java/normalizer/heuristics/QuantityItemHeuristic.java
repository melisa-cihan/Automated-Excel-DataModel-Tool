package normalizer.heuristics;

import normalizer.heuristics.HeuristicRule;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Heuristic rule to split a string like "2 books" into Quantity and Item columns.
 * Implements the normalizer.heuristics.HeuristicRule interface.
 */
public class QuantityItemHeuristic implements HeuristicRule {
    private static final Pattern NUMERIC_PREFIX_PATTERN = Pattern.compile("^(\\d+)\\s+(.*)$", Pattern.CASE_INSENSITIVE);

    @Override
    public boolean apply(String originalColumnName, Object cellValue, Map<String, Object> newRow) {
// This heuristic only applies to String values.
        if (!(cellValue instanceof String stringValue)) {
            return false; // Not a string, so this heuristic cannot apply.
        }

        String trimmedStringValue = stringValue.trim();
        Matcher numericMatcher = NUMERIC_PREFIX_PATTERN.matcher(trimmedStringValue);

        if (numericMatcher.find()) { // If the pattern matches (e.g., "2 books")
            try {
                // Extract quantity and item, and add them as new columns to the newRow map.
                newRow.put(originalColumnName + "_Quantity", Integer.parseInt(numericMatcher.group(1)));
                newRow.put(originalColumnName + "_Item", numericMatcher.group(2).trim());
                return true; // Successfully applied this heuristic.
            } catch (NumberFormatException e) {
                // If the "quantity" part isn't a valid number (e.g., "Two books"),
                // this heuristic doesn't apply, so return false.
                return false;
            }
        }

        return false; // Pattern did not match, so this heuristic did not apply.
    }
}
