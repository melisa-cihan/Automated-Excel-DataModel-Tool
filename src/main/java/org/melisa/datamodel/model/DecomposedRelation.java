package org.melisa.datamodel.model;

import java.util.List;
import java.util.Map;

/**
 * A Java Record representing a single decomposed relation (table) resulting
 * from the normalization process.
 * Stores the table name, its data rows, and key constraints required for SQL
 * generation.
 *
 * @param name        The name of the resulting relation (used for SQL table
 *                    naming).
 * @param data        The rows (List of Maps) belonging to this relation, where
 *                    each map maps column names to values.
 * @param primaryKeys A list of SQL-sanitized column names that form the primary
 *                    key for this relation.
 * @param foreignKeys A map where the key is the local foreign key column name
 *                    (SQL-sanitized)
 *                    and the value is the reference string (e.g.,
 *                    "REFERENCED_TABLE(COLUMN_NAME)").
 */
public record DecomposedRelation(
                String name,
                List<Map<String, Object>> data,
                List<String> primaryKeys,
                Map<String, String> foreignKeys) {

}
