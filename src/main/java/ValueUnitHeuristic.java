import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Heuristic rule to split a string like "50 kg" or "100 USD" into Value and Unit columns.
 * Implements the HeuristicRule interface.
 */
public class ValueUnitHeuristic implements HeuristicRule {
    private static final Pattern VALUE_UNIT_PATTERN = Pattern.compile("^(-?\\d+(\\.\\d+)?)\\s*([a-zA-Z%]+)$");

    @Override
    public boolean apply(String originalColumnName, Object cellValue, Map<String, Object> newRow) {
        if (!(cellValue instanceof String stringValue)) {
            return false;
        }

        String trimmedStringValue = stringValue.trim();
        Matcher valueUnitMatcher = VALUE_UNIT_PATTERN.matcher(trimmedStringValue);

        if (valueUnitMatcher.find()) {
            try {
                // Put value as Double for flexibility (e.g., 15.7 cm)
                newRow.put(originalColumnName + "_Value", Double.parseDouble(valueUnitMatcher.group(1)));
                // Group 3 captures the unit (e.g., "kg", "USD", "%")
                newRow.put(originalColumnName + "_Unit", valueUnitMatcher.group(3).trim());
                return true;
            } catch (NumberFormatException e) {
                return false;
            }
        }
        return false;
    }
}
