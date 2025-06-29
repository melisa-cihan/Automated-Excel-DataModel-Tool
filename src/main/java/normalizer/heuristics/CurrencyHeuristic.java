package normalizer.heuristics;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Heuristic rule to split a string like "50 €" or "60 $" into Amount and Currency columns.
 */
public class CurrencyHeuristic implements HeuristicRule {
    private static final Pattern CURRENCY_PATTERN = Pattern.compile("^(-?\\d+(\\.\\d+)?)\\s*([$€£¥₹₩¢]+)$");
    @Override
    public boolean apply(String originalColumnName, Object cellValue, Map<String, Object> newRow) {
        if (!(cellValue instanceof String stringValue)) {
            return false;
        }

        String trimmedStringValue = stringValue.trim();
        Matcher matcher = CURRENCY_PATTERN.matcher(trimmedStringValue);

        if (matcher.find()) {
            try {
                newRow.put(originalColumnName + "_Amount", Double.parseDouble(matcher.group(1)));
                newRow.put(originalColumnName + "_Currency", matcher.group(3).trim()); // Group 3 is the currency symbol
                return true;
            } catch (NumberFormatException e) {
                return false; // Not a valid number part
            }
        }
        return false;
    }
}
