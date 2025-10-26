import java.util.*;
import java.util.stream.Collectors;

/**
 * SecondNormalizer is responsible for transforming a dataset from First Normal Form (1NF)
 * into Second Normal Form (2NF). This process involves identifying candidate keys,
 * analyzing functional dependencies, and decomposing the original relation to eliminate
 * partial dependencies (where a non-key attribute is dependent only on a subset of a composite key).
 *
 * This class adheres to the Single Responsibility Principle, focusing solely on relational decomposition.
 */
public class SecondNormalizer {

    private static final String MAIN_RELATION_NAME = "MainRelation";

    /**
     * Public interface to begin the 2NF normalization process.
     *
     * @param input1NFData The list of maps representing the data that is already in 1NF.
     * Each Map is a tuple, and keys are column names.
     * @return A map where keys are relation names (e.g., "MainRelation", "ProductDetails")
     * and values are the new decomposed list of tuples (tables).
     */
    public Map<String, List<Map<String, Object>>> normalizeTo2NF(
            List<Map<String, Object>> input1NFData) {

        if (input1NFData == null || input1NFData.isEmpty()) {
            return Collections.emptyMap();
        }

        // Step 1: Synthesize the Primary/Candidate Key using the dedicated external analyzer.
        CandidateKeyIdentifier identifier = new CandidateKeyIdentifier();
        Set<Set<String>> allCandidateKeys = identifier.identifyAllCandidateKeys(input1NFData);

        // Select one Candidate Key for the decomposition process.
        Set<String> candidateKey = selectKeyFor2NFDecomposition(allCandidateKeys);

        if (candidateKey.isEmpty()) {
            System.err.println("Error: No Candidate Key could be identified for the relation.");
            Map<String, List<Map<String, Object>>> result = new HashMap<>();
            result.put(MAIN_RELATION_NAME, input1NFData);
            return result;
        }

        System.out.println("Selected Candidate Key for 2NF: " + candidateKey);

        // Step 2: Identify and Decompose Partial Dependencies.
        // This is the core 2NF logic.
        return decomposeForPartialDependencies(input1NFData, candidateKey);
    }

    /**
     * Selects the most appropriate Candidate Key for 2NF decomposition demonstration.
     * We prefer a composite key (size > 1) if available, as 2NF is only relevant for composite keys.
     *
     * @param allCandidateKeys All identified minimal candidate keys.
     * @return The selected key (composite key if available, otherwise the smallest key).
     */
    private Set<String> selectKeyFor2NFDecomposition(Set<Set<String>> allCandidateKeys) {
        if (allCandidateKeys.isEmpty()) {
            return Collections.emptySet();
        }

        // 1. Try to find the smallest composite key (size > 1)
        Set<String> smallestCompositeKey = allCandidateKeys.stream()
                .filter(key -> key.size() > 1)
                .min(Comparator.comparingInt(Set::size))
                .orElse(null);

        if (smallestCompositeKey != null) {
            return smallestCompositeKey;
        }

        // 2. If no composite key exists, return the smallest simple key.
        return allCandidateKeys.stream()
                .min(Comparator.comparingInt(Set::size))
                .orElseGet(Collections::emptySet);
    }

    /**
     * Performs the relational decomposition to eliminate partial dependencies.
     *
     * @param input1NFData The input relation (List of Maps).
     * @param candidateKey The identified composite primary key.
     * @return A map of new, decomposed relations.
     */
    private Map<String, List<Map<String, Object>>> decomposeForPartialDependencies(
            List<Map<String, Object>> input1NFData,
            Set<String> candidateKey) {

        Map<String, List<Map<String, Object>>> normalizedRelations = new HashMap<>();

        // If the key is not composite (single column), the relation is automatically in 2NF.
        if (candidateKey.size() <= 1) {
            normalizedRelations.put(MAIN_RELATION_NAME, input1NFData);
            return normalizedRelations;
        }

        // The determinant (a subset of the composite key)
        final String partialDeterminant = new ArrayList<>(candidateKey).get(0);

        // Temporary variable to find the partially dependent attribute
        String dependentAttrTemp = null;

        // Simplified Heuristic: Find an attribute that is non-key but is identical
        // for every unique value of the partial determinant.
        for (String column : input1NFData.get(0).keySet()) {
            if (!candidateKey.contains(column)) {
                // Check if it's determined only by the first part of the key
                if (isPartiallyDependent(input1NFData, partialDeterminant, column)) {
                    dependentAttrTemp = column;
                    break;
                }
            }
        }

        // --- FIX: Assign to a final variable for use in the Stream lambdas ---
        final String partiallyDependentAttribute = dependentAttrTemp;

        if (partiallyDependentAttribute == null) {
            // No partial dependencies found/simulated, relation is 2NF.
            normalizedRelations.put(MAIN_RELATION_NAME, input1NFData);
            return normalizedRelations;
        }

        // --- DECOMPOSITION EXECUTION ---

        // Relation 1: The partially dependent relation (e.g., OrderDetails)
        // Key: {partialDeterminant} -> Non-Key: {partiallyDependentAttribute}
        Set<String> r1Columns = new LinkedHashSet<>();
        r1Columns.add(partialDeterminant);
        r1Columns.add(partiallyDependentAttribute);

        List<Map<String, Object>> r1Data = input1NFData.stream()
                .map(row -> {
                    Map<String, Object> newRow = new LinkedHashMap<>();
                    newRow.put(partialDeterminant, row.get(partialDeterminant));
                    // Now safe to use 'partiallyDependentAttribute' in the lambda
                    newRow.put(partiallyDependentAttribute, row.get(partiallyDependentAttribute));
                    return newRow;
                })
                .distinct() // Remove redundant rows
                .collect(Collectors.toList());

        normalizedRelations.put(partialDeterminant + "_Details", r1Data);
        System.out.println("Decomposed Relation: " + partialDeterminant + "_Details created.");

        // Relation 2: The residual relation (Main Relation)
        // Remove the partially dependent attribute from the main table, keeping the key and other attributes.
        List<Map<String, Object>> residualData = input1NFData.stream()
                .map(row -> {
                    Map<String, Object> newRow = new LinkedHashMap<>(row);
                    // Now safe to use 'partiallyDependentAttribute' in the lambda
                    newRow.remove(partiallyDependentAttribute); // Eliminate redundancy
                    return newRow;
                })
                .collect(Collectors.toList());

        normalizedRelations.put(MAIN_RELATION_NAME, residualData);

        return normalizedRelations;
    }

    /**
     * Simplified heuristic to detect partial dependency for the purpose of demonstrating decomposition.
     * Checks if attribute 'dependentAttr' is functionally dependent on 'determinant' (i.e., every
     * unique value of determinant maps to only one unique value of dependentAttr).
     */
    private boolean isPartiallyDependent(List<Map<String, Object>> data, String determinant, String dependentAttr) {
        Map<Object, Object> checkMap = new HashMap<>();

        for (Map<String, Object> row : data) {
            Object determinantValue = row.get(determinant);
            Object dependentValue = row.get(dependentAttr);

            if (checkMap.containsKey(determinantValue)) {
                // If a determinant value already exists, check if the dependent value is consistent
                if (!checkMap.get(determinantValue).equals(dependentValue)) {
                    // Inconsistency found: Not a functional dependency.
                    return false;
                }
            } else {
                checkMap.put(determinantValue, dependentValue);
            }
        }
        // If the loop completes, it's a functional dependency on the determinant.
        // We assume here this is a partial dependency because we only checked the first key part.
        return true;
    }
}
