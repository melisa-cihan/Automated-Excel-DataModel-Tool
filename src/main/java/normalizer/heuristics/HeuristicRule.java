package normalizer.heuristics;

import java.util.Map;
public interface HeuristicRule {

    /**
     * Applies this heuristic rule to a given cell's value within the context of its original column.
     *
     * @param originalColumnName The original name of the column where the cellValue was found.
     * This is used for generating new column names (e.g., "Product_Quantity").
     * @param cellValue The original value of the cell to be processed by this heuristic.
     * Implementations should typically only process String values, but the parameter
     * is Object to match the general data structure.
     * @param newRow A LinkedHashMap representing the row currently being built. If the heuristic
     * applies a transformation, it should add its results (e.g., new columns or
     * transformed values) to this map.
     * @return true if the heuristic successfully applied a transformation to the cellValue
     * (meaning the original value should NOT be added to newRow by the caller),
     * false if this heuristic did not apply or failed to transform (meaning the caller
     * should proceed to the next heuristic or add the original value to newRow).
     */
    boolean apply(String originalColumnName, Object cellValue, Map<String, Object> newRow);

}
