package org.melisa.datamodel.normalization.heuristics;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Heuristic rule to split a string like "Primary (Alias)" into Primary and Alias columns.
 * Implements the HeuristicRule interface.
 */
public class ParentheticalAliasHeuristic implements HeuristicRule{
    private static final Pattern PARENTHETICAL_ALIAS_PATTERN = Pattern.compile("^(.*?)\\s*\\((.*?)\\)$");

    @Override
    public boolean apply(String originalColumnName, Object cellValue, Map<String, Object> newRow) {
        if (!(cellValue instanceof String stringValue)) {
            return false;
        }

        String trimmedStringValue = stringValue.trim();
        Matcher aliasMatcher = PARENTHETICAL_ALIAS_PATTERN.matcher(trimmedStringValue);

        if (aliasMatcher.find()) {

            newRow.put(originalColumnName + "_Primary", aliasMatcher.group(1).trim());
            newRow.put(originalColumnName + "_Alias", aliasMatcher.group(2).trim());
            return true;
        }

        return false;
    }
}
