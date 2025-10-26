import java.util.List;
import java.util.Map;

/**
 * A Java Record representing a single decomposed relation (table) resulting from the normalization
 * process. Records automatically provide the constructor, accessors (e.g., name()), equals(),
 * hashCode(), and toString().
 *
 * @param name The name of the resulting relation (used for SQL table naming).
 * @param data The rows (List of Maps) belonging to this relation.
 * @param primaryKeys A list of SQL-sanitized column names that form the primary key.
 * @param foreignKeys A map where the key is the local foreign key column name (SQL-sanitized)
 * and the value is the reference string (e.g., "REFERENCED_TABLE(COLUMN_NAME)").
 */
public record DecomposedRelation(
        String name,
        List<Map<String, Object>> data,
        List<String> primaryKeys,
        Map<String, String> foreignKeys) {
    // No explicit constructor or getters needed!
}
