import java.time.LocalDateTime;
import java.util.ArrayList; // Import ArrayList
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Generates SQL CREATE TABLE and INSERT statements from normalized data.
 * This class infers SQL data types from Java objects and, crucially,
 * from String values that represent other types (e.g., "123", "true").
 *
 * This version uses standard, portable SQL types (INTEGER, DECIMAL, SMALLINT)
 * instead of dialect-specific types for maximum database compatibility.
 */
public class SqlGenerator {

    // Regex for common ISO date/datetime string formats
    private static final Pattern SQL_DATE_PATTERN = Pattern.compile("^\\d{4}-\\d{2}-\\d{2}(T\\d{2}:\\d{2}(:\\d{2}(\\.\\d+)?)?)?$");
    private static final String DEFAULT_STRING_TYPE = "VARCHAR(255)";
    private static final String TYPE_INTEGER = "INTEGER";
    private static final String TYPE_DECIMAL = "DECIMAL(18, 4)";
    private static final String TYPE_BOOLEAN = "SMALLINT";
    private static final String TYPE_DATE = "DATE";
    private static final String TYPE_TIMESTAMP = "TIMESTAMP";

    /**
     * Converts a string name to a SQL-friendly identifier (e.g., UPPERCASE_WITH_UNDERSCORES).
     *
     * @param name The original column name.
     * @return A sanitized, SQL-safe identifier.
     */
    public static String toSqlIdentifier(String name) {
        if (name == null || name.trim().isEmpty()) {
            return "UNKNOWN_COLUMN";
        }
        String sanitized = name.trim().replaceAll("[^a-zA-Z0-9_]", "_");
        if (sanitized.matches("^\\d.*")) {
            sanitized = "_" + sanitized;
        }
        sanitized = sanitized.replaceAll("^_|_$", "");
        if (sanitized.isEmpty()) {
            return "DEFAULT_COLUMN";
        }
        return sanitized.toUpperCase();
    }

    /**
     * Determines the most appropriate SQL data type for a given Java object value.
     * This method handles both pre-typed objects (Integer, Double) and
     * infers types from String objects, mapping them to standard SQL types.
     *
     * @param value The Java object value from the data map.
     * @return A string representing the SQL data type (e.g., "INTEGER", "DECIMAL(18, 4)").
     */
    private static String getSqlType(Object value) {
        if (value == null) {
            return DEFAULT_STRING_TYPE; // Default for nulls
        }

        // Use modern switch expression for clean type matching
        return switch (value) {
            // Case 1: The Normalizer's heuristics already typed the object correctly.
            // FIX: Use unique, named variables (i, l, d, f, b, ldt)
            // as unnamed patterns (_) are not standard in Java 21.
            case Integer i -> TYPE_INTEGER;
            case Long l -> TYPE_INTEGER;
            case Double d -> TYPE_DECIMAL;
            case Float f -> TYPE_DECIMAL;
            case Boolean b -> TYPE_BOOLEAN;
            case LocalDateTime ldt -> TYPE_TIMESTAMP;

            // Case 2: The object is a String that needs type inference.
            case String strValue -> inferSqlTypeFromString(strValue);

            // Fallback for any other unexpected Java types
            default -> DEFAULT_STRING_TYPE;
        };
    }

    /**
     * Helper method to infer the SQL type from a String value.
     * Checks for Boolean, Date, Integer, and Double before defaulting to VARCHAR.
     *
     * @param value The String value to analyze.
     * @return The inferred SQL data type as a String.
     */
    private static String inferSqlTypeFromString(String value) {
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            return DEFAULT_STRING_TYPE;
        }

        // Check 1: Boolean
        if (trimmed.equalsIgnoreCase("true") || trimmed.equalsIgnoreCase("false")) {
            return TYPE_BOOLEAN;
        }

        // Check 2: Date/Datetime
        if (SQL_DATE_PATTERN.matcher(trimmed).matches()) {
            return trimmed.contains("T") ? TYPE_TIMESTAMP : TYPE_DATE;
        }

        // Check 3: Numeric (Integer or Double)
        try {
            // Try parsing as a whole number
            Long.parseLong(trimmed);
            return TYPE_INTEGER; // Use Long to handle large numbers, but map to SQL INTEGER
        } catch (NumberFormatException e1) {
            // Not an integer, try parsing as a floating-point number
            try {
                Double.parseDouble(trimmed);
                return TYPE_DECIMAL; // Use DECIMAL for floating-point precision
            } catch (NumberFormatException e2) {
                // Not a number, fall through to default
            }
        }

        // Default: Treat as a standard text string
        return DEFAULT_STRING_TYPE;
    }


    /**
     * Promotes a SQL data type to a more general one if a conflicting type is found.
     * This is crucial for columns with mixed data (e.g., "10" and "10.5").
     *
     * @param existingType The currently inferred SQL type for a column.
     * @param newType      The new type encountered for the same column.
     * @return The most general (or compatible) SQL type.
     */
    private static String promoteSqlType(String existingType, String newType) {
        if (existingType.equals(newType)) {
            return existingType;
        }

        // Promotion priority: VARCHAR > DECIMAL > INTEGER > SMALLINT
        // (Date types are handled separately)

        if (existingType.equals(DEFAULT_STRING_TYPE) || newType.equals(DEFAULT_STRING_TYPE)) {
            return DEFAULT_STRING_TYPE;
        }
        if (existingType.equals(TYPE_DECIMAL) || newType.equals(TYPE_DECIMAL)) {
            // Promote INTEGER/SMALLINT to DECIMAL if mixed
            if (newType.equals(TYPE_INTEGER) || existingType.equals(TYPE_INTEGER) || newType.equals(TYPE_BOOLEAN) || existingType.equals(TYPE_BOOLEAN)) {
                return TYPE_DECIMAL;
            }
        }
        if (existingType.equals(TYPE_INTEGER) || newType.equals(TYPE_INTEGER)) {
            // Promote SMALLINT to INTEGER if mixed
            if (newType.equals(TYPE_BOOLEAN) || existingType.equals(TYPE_BOOLEAN)) {
                return TYPE_INTEGER;
            }
        }
        if (existingType.equals(TYPE_TIMESTAMP) || newType.equals(TYPE_TIMESTAMP)) {
            // Promote DATE to TIMESTAMP if mixed
            if (newType.equals(TYPE_DATE) || existingType.equals(TYPE_DATE)) {
                return TYPE_TIMESTAMP;
            }
        }
        // If types are different but not promotable (e.g., INTEGER and DATE), default to VARCHAR
        if (!existingType.equals(newType)) {
            return DEFAULT_STRING_TYPE;
        }

        return existingType; // Should be covered by the first check
    }


    /**
     * Generates a SQL script containing CREATE TABLE and INSERT statements
     * based on the normalized data and including key constraints.
     *
     * @param normalizedData The List of data rows (maps).
     * @param tableName      The desired name for the SQL table.
     * @param primaryKeys    A List of SQL-sanitized column names forming the primary key.
     * @param foreignKeys    A Map where the key is the FK column name (SQL-sanitized) and
     * the value is the reference string (e.g., "REFERENCE_TABLE(COLUMN)").
     * @return A String containing the full SQL script.
     */
    public static String generateSqlScript(
            List<Map<String, Object>> normalizedData,
            String tableName,
            List<String> primaryKeys,
            Map<String, String> foreignKeys) {

        StringBuilder sqlBuilder = new StringBuilder();

        if (normalizedData == null || normalizedData.isEmpty()) {
            return "-- No data to generate SQL for.\n";
        }

        String sqlTableName = toSqlIdentifier(tableName);

        // --- Step 1: Determine Comprehensive Schema (All Columns and Most General Types) ---
        Map<String, String> columnSchema = new LinkedHashMap<>();

        for (Map<String, Object> row : normalizedData) {
            for (Map.Entry<String, Object> entry : row.entrySet()) {
                String originalColumnName = entry.getKey();
                String sqlColumnName = toSqlIdentifier(originalColumnName);
                String inferredType = getSqlType(entry.getValue());

                if (!columnSchema.containsKey(sqlColumnName)) {
                    columnSchema.put(sqlColumnName, inferredType);
                } else {
                    String existingType = columnSchema.get(sqlColumnName);
                    String promotedType = promoteSqlType(existingType, inferredType);
                    columnSchema.put(sqlColumnName, promotedType);
                }
            }
        }

        // --- Step 2: Generate CREATE TABLE Statement (Refactored for Cleanliness) ---
        sqlBuilder.append("CREATE TABLE ").append(sqlTableName).append(" (\n");

        // Use a List to manage definitions, avoiding trailing comma bugs.
        List<String> createDefinitions = new ArrayList<>();

        // Add all column definitions (Name and Type)
        for (Map.Entry<String, String> entry : columnSchema.entrySet()) {
            StringBuilder columnDef = new StringBuilder();
            columnDef.append("    ").append(entry.getKey()).append(" ").append(entry.getValue());
            // Add NOT NULL constraint if column is part of the primary key
            if (primaryKeys != null && primaryKeys.contains(entry.getKey())) {
                columnDef.append(" NOT NULL");
            }
            createDefinitions.add(columnDef.toString());
        }

        // Add PRIMARY KEY constraint
        if (primaryKeys != null && !primaryKeys.isEmpty()) {
            String pkList = String.join(", ", primaryKeys);
            createDefinitions.add("    CONSTRAINT PK_" + sqlTableName + " PRIMARY KEY (" + pkList + ")");
        }

        // Add FOREIGN KEY constraints
        if (foreignKeys != null && !foreignKeys.isEmpty()) {
            for (Map.Entry<String, String> fkEntry : foreignKeys.entrySet()) {
                String fkColumn = fkEntry.getKey();
                String fkReference = fkEntry.getValue();
                String fkName = "FK_" + sqlTableName + "_" + fkColumn;

                StringBuilder fkDef = new StringBuilder();
                fkDef.append("    CONSTRAINT ").append(fkName);
                fkDef.append(" FOREIGN KEY (").append(fkColumn).append(")");
                fkDef.append(" REFERENCES ").append(fkReference);
                createDefinitions.add(fkDef.toString());
            }
        }

        // Join all definitions with a comma and newline
        sqlBuilder.append(String.join(",\n", createDefinitions));
        sqlBuilder.append("\n);\n\n");


        // --- Step 3: Generate INSERT Statements ---
        for (Map<String, Object> row : normalizedData) {
            sqlBuilder.append("INSERT INTO ").append(sqlTableName).append(" (");

            StringBuilder cols = new StringBuilder();
            StringBuilder values = new StringBuilder();

            // Iterate through the determined schema to ensure all columns are included in order
            for (String sqlColumnName : columnSchema.keySet()) {
                String originalColumnName = null;
                for (String keyInRow : row.keySet()) {
                    if (toSqlIdentifier(keyInRow).equals(sqlColumnName)) {
                        originalColumnName = keyInRow;
                        break;
                    }
                }

                Object value = (originalColumnName != null) ? row.get(originalColumnName) : null;

                if (cols.length() > 0) {
                    cols.append(", ");
                    values.append(", ");
                }
                cols.append(sqlColumnName);
                values.append(formatSqlValue(value));
            }
            sqlBuilder.append(cols).append(")\nVALUES (").append(values).append(");\n");
        }

        return sqlBuilder.toString();
    }

    /**
     * Formats a Java object value into a SQL literal string for INSERT statements.
     *
     * @param value The Java object value (e.g., Integer, Double, String).
     * @return A SQL-formatted string representation of the value (e.g., 123, 'Hello').
     */
    private static String formatSqlValue(Object value) {
        if (value == null) {
            return "NULL";
        }

        // Numbers should NOT be quoted.
        // Booleans are converted to 1 (true) or 0 (false) for SMALLINT.
        // All others (including dates/datetimes which are treated as strings) MUST be quoted.
        return switch (value) {
            case String strValue -> "'" + strValue.replace("'", "''") + "'";
            case Boolean boolValue -> boolValue ? "1" : "0"; // Use 1/0 for SMALLINT
            case Number numValue -> numValue.toString();
            // Default: Quote any other object's toString() representation
            default -> "'" + value.toString().replace("'", "''") + "'";
        };
    }
}

