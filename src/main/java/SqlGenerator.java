import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class SqlGenerator {

    // Regex for common date string formats for SQL type inference
    private static final Pattern SQL_DATE_PATTERN = Pattern.compile("^\\d{4}-\\d{2}-\\d{2}(T\\d{2}:\\d{2}(:\\d{2}(\\.\\d+)?)?)?$");

    /**
     * Converts a string name to a SQL-friendly identifier.
     * Replaces non-alphanumeric characters (except underscore) with underscores,
     * ensures it doesn't start with a digit, and converts to uppercase.
     *
     * @param name The original column name.
     * @return A sanitized SQL identifier.
     */
    private static String toSqlIdentifier(String name) {
        if (name == null || name.trim().isEmpty()) {
            return "UNKNOWN_COLUMN"; // Fallback for empty names, though getColumnNames should handle this
        }
        // Replace any characters not a letter, number, or underscore with an underscore
        String sanitized = name.trim().replaceAll("[^a-zA-Z0-9_]", "_");
        // Ensure it doesn't start with a digit (SQL identifiers generally cannot)
        if (sanitized.matches("^\\d.*")) {
            sanitized = "_" + sanitized;
        }
        // Remove leading/trailing underscores that might result from sanitization
        sanitized = sanitized.replaceAll("^_|_$", "");
        // If it becomes empty after sanitization (e.g., original was just " "), assign a default
        if (sanitized.isEmpty()) {
            return "DEFAULT_COLUMN";
        }
        return sanitized.toUpperCase(); // Common SQL convention for identifiers
    }

    /**
     * Determines the most appropriate SQL data type for a given Java object value.
     * Uses a switch expression for cleaner type checking.
     * @param value The Java object value.
     * @return A string representing the SQL data type (e.g., "INT", "VARCHAR(255)").
     */
    private static String getSqlType(Object value) {
        if (value == null) {
            return "VARCHAR(255)"; // Default for null, will be promoted if other non-null values appear
        }

        return switch (value) {
            case Integer ignored -> "INT";
            case Double ignored -> "DOUBLE";
            case Boolean ignored -> "BOOLEAN";
            case String strValue -> { // Pattern variable 'strValue' automatically casts 'value' to String
                // Check for date/datetime patterns within the string
                if (SQL_DATE_PATTERN.matcher(strValue).matches()) {
                    if (strValue.contains("T")) {
                        yield "DATETIME"; // 'yield' is used in switch expressions to return a value
                    }
                    yield "DATE";
                }
                yield "VARCHAR(255)"; // Default for other strings
            }
            case LocalDateTime ignored -> "DATETIME"; // Handle LocalDateTime objects directly if they somehow appear
            default -> "VARCHAR(255)"; // Fallback for any other unexpected Java types
        };
    }


    /**
     * Promotes a SQL data type to a more general one if a conflicting type is found.
     * E.g., INT -> DOUBLE, DATE -> DATETIME, VARCHAR(X) -> VARCHAR(Y)
     * This is a simplified promotion logic.
     *
     * @param existingType The currently inferred SQL type for a column.
     * @param newType      The new type encountered for the same column.
     * @return The most general (or compatible) SQL type.
     */
    private static String promoteSqlType(String existingType, String newType) {
        if (existingType.equals(newType)) {
            return existingType;
        }

        // Simple type promotion rules
        if (existingType.equals("VARCHAR(255)") || newType.equals("VARCHAR(255)")) {
            return "VARCHAR(255)"; // String is the most general for mixed types
        }
        if ((existingType.equals("INT") && newType.equals("DOUBLE")) || (existingType.equals("DOUBLE") && newType.equals("INT"))) {
            return "DOUBLE"; // Double is more general than Int
        }
        if ((existingType.equals("DATE") && newType.equals("DATETIME")) || (existingType.equals("DATETIME") && newType.equals("DATE"))) {
            return "DATETIME"; // DATETIME is more general than DATE
        }
        // Add more complex promotion rules if necessary (e.g., for DECIMAL precision)

        // If no specific promotion rule, default to the more general type (VARCHAR as a safe bet)
        return "VARCHAR(255)";
    }


    /**
     * Generates a SQL script containing CREATE TABLE and INSERT statements
     * based on the normalized data.
     *
     * @param normalizedData The data in 1NF (List of Maps).
     * @param tableName The desired name for the SQL table.
     * @return A String containing the SQL script.
     */
    public static String generateSqlScript(List<Map<String, Object>> normalizedData, String tableName) {
        StringBuilder sqlBuilder = new StringBuilder();

        if (normalizedData == null || normalizedData.isEmpty()) {
            return "-- No data to generate SQL for.\n";
        }

        // Sanitize table name (similar to column names)
        String sqlTableName = toSqlIdentifier(tableName);

        // --- Step 1: Determine Comprehensive Schema (All Columns and Most General Types) ---
        // Use LinkedHashMap to preserve the order of columns as they are first encountered
        Map<String, String> columnSchema = new LinkedHashMap<>();

        // Iterate through ALL normalized rows to discover all columns and infer their most general type
        for (Map<String, Object> row : normalizedData) {
            for (Map.Entry<String, Object> entry : row.entrySet()) {
                String originalColumnName = entry.getKey();
                String sqlColumnName = toSqlIdentifier(originalColumnName);
                String inferredType = getSqlType(entry.getValue());

                if (!columnSchema.containsKey(sqlColumnName)) {
                    // If column is new, add it with its inferred type
                    columnSchema.put(sqlColumnName, inferredType);
                } else {
                    // If column already exists, promote its type if the new type is more general
                    String existingType = columnSchema.get(sqlColumnName);
                    String promotedType = promoteSqlType(existingType, inferredType);
                    columnSchema.put(sqlColumnName, promotedType);
                }
            }
        }

        // --- Step 2: Generate CREATE TABLE Statement ---
        sqlBuilder.append("CREATE TABLE ").append(sqlTableName).append(" (\n");
        boolean firstColumn = true;
        for (Map.Entry<String, String> entry : columnSchema.entrySet()) {
            if (!firstColumn) {
                sqlBuilder.append(",\n"); // Add comma before subsequent columns
            }
            sqlBuilder.append("    ").append(entry.getKey()).append(" ").append(entry.getValue());
            firstColumn = false;
        }
        sqlBuilder.append("\n);\n\n");

        // --- Step 3: Generate INSERT Statements ---
        for (Map<String, Object> row : normalizedData) {
            sqlBuilder.append("INSERT INTO ").append(sqlTableName).append(" (");

            StringBuilder cols = new StringBuilder();
            StringBuilder values = new StringBuilder();
            boolean firstValue = true;

            // Iterate through the *determined schema* to ensure all columns are included
            // and in the correct order for the INSERT statement
            for (Map.Entry<String, String> schemaEntry : columnSchema.entrySet()) {
                String sqlColumnName = schemaEntry.getKey();
                String originalColumnName = null; // We need to find the original key from the row map

                // Find the original column name that maps to this sanitized SQL column name
                // This is needed because `row` map keys are original names
                for (String keyInRow : row.keySet()) {
                    if (toSqlIdentifier(keyInRow).equals(sqlColumnName)) {
                        originalColumnName = keyInRow;
                        break;
                    }
                }

                // Get the value from the current row. If the row doesn't have this specific column
                // (which can happen if a column was created by normalization in another row), it will be null.
                Object value = (originalColumnName != null && row.containsKey(originalColumnName)) ? row.get(originalColumnName) : null;

                if (!firstValue) {
                    cols.append(", ");
                    values.append(", ");
                }
                cols.append(sqlColumnName);
                values.append(formatSqlValue(value));
                firstValue = false;
            }
            sqlBuilder.append(cols).append(")\nVALUES (").append(values).append(");\n");
        }

        return sqlBuilder.toString();
    }

    /**
     * Formats a Java object value into a SQL literal string for INSERT statements.
     * Handles nulls, strings (with quoting and escaping), booleans, and numbers.
     *
     * @param value The Java object value.
     * @return A SQL-formatted string representation of the value.
     */
    private static String formatSqlValue(Object value) {
        if (value == null) {
            return "NULL";
        }
        // Use switch expression for cleaner type checking and formatting
        return switch (value) {
            case String strValue -> "'" + strValue.replace("'", "''") + "'";
            case Boolean boolValue -> boolValue ? "TRUE" : "FALSE";
            case Number numValue -> numValue.toString(); // Integer, Double, etc. are all Numbers
            // Fallback for any other types, convert to string and quote.
            // This catches types like LocalDateTime if they aren't handled by specific cases.
            default -> "'" + value.toString().replace("'", "''") + "'";
        };
    }
}
