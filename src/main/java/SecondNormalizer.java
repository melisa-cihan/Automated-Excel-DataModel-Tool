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
     * Helper to sanitize column names using the logic from SqlGenerator,
     * ensuring keys are stored in the correct format (UPPERCASE).
     */
    private String toSqlIdentifier(String name) {
        // Since SqlGenerator is in the same package, we can call its static method directly.
        return SqlGenerator.toSqlIdentifier(name);
    }

    /**
     * Public interface to begin the 2NF normalization process.
     *
     * @param input1NFData The list of maps representing the data that is already in 1NF.
     * @return A list of DecomposedRelation objects, each containing the data and key metadata (PK/FK).
     */
    public List<DecomposedRelation> normalizeTo2NF(
            List<Map<String, Object>> input1NFData) {

        if (input1NFData == null || input1NFData.isEmpty()) {
            return Collections.emptyList();
        }

        // Step 1: Synthesize the Primary/Candidate Key using the dedicated external analyzer.
        // NOTE: CandidateKeyIdentifier must be implemented elsewhere.
        CandidateKeyIdentifier identifier = new CandidateKeyIdentifier();
        Set<Set<String>> allCandidateKeys = identifier.identifyAllCandidateKeys(input1NFData);

        // Select one Candidate Key for the decomposition process.
        Set<String> candidateKey = selectKeyFor2NFDecomposition(allCandidateKeys);

        if (candidateKey.isEmpty()) {
            System.err.println("Error: No Candidate Key could be identified for the relation. Returning original data as MainRelation.");

            // If no key found, return the original data wrapped in a DecomposedRelation with no keys.
            List<String> emptyKeys = Collections.emptyList();
            Map<String, String> emptyFks = Collections.emptyMap();

            return List.of(new DecomposedRelation(MAIN_RELATION_NAME, input1NFData, emptyKeys, emptyFks));
        }

        System.out.println("Selected Candidate Key for 2NF: " + candidateKey);

        // Step 2: Identify and Decompose Partial Dependencies.
        return decomposeForPartialDependencies(input1NFData, candidateKey);
    }

    /**
     * Selects the most appropriate Candidate Key for 2NF decomposition demonstration.
     * We prefer a composite key (size > 1) if available, as 2NF is only relevant for composite keys.
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
     * Performs the relational decomposition to eliminate partial dependencies,
     * and correctly identifies the Primary Key (PK) and Foreign Key (FK) for each new relation.
     *
     * @param input1NFData The input relation (List of Maps).
     * @param candidateKey The identified composite primary key (e.g., [MiNr, ProNr]).
     * @return A list of new, decomposed relations with metadata.
     */
    private List<DecomposedRelation> decomposeForPartialDependencies(
            List<Map<String, Object>> input1NFData,
            Set<String> candidateKey) {

        List<DecomposedRelation> normalizedRelations = new ArrayList<>();

        // If the key is not composite (single column), the relation is automatically in 2NF.
        if (candidateKey.size() <= 1) {
            // Relation is already in 2NF. Return it with its PK.
            List<String> pk = candidateKey.stream().map(this::toSqlIdentifier).collect(Collectors.toList());
            normalizedRelations.add(new DecomposedRelation(MAIN_RELATION_NAME, input1NFData, pk, Collections.emptyMap()));
            return normalizedRelations;
        }

        // --- SIMPLIFIED 2NF HEURISTIC ---
        // We select the first column of the composite key as the determinant.
        final String partialDeterminant = new ArrayList<>(candidateKey).get(0);

        String dependentAttrTemp = null;
        for (String column : input1NFData.get(0).keySet()) {
            if (!candidateKey.contains(column)) {
                if (isPartiallyDependent(input1NFData, partialDeterminant, column)) {
                    dependentAttrTemp = column;
                    break;
                }
            }
        }

        final String partiallyDependentAttribute = dependentAttrTemp;

        if (partiallyDependentAttribute == null) {
            // No partial dependencies found/simulated, relation is 2NF.
            List<String> pk = candidateKey.stream().map(this::toSqlIdentifier).collect(Collectors.toList());
            normalizedRelations.add(new DecomposedRelation(MAIN_RELATION_NAME, input1NFData, pk, Collections.emptyMap()));
            return normalizedRelations;
        }

        // --- DECOMPOSITION EXECUTION & KEY ASSIGNMENT ---

        // Relation 1: The partially dependent relation (e.g., ProNr_Details or Worker_Details)
        final String detailsRelationName = partialDeterminant + "_Details";

        List<Map<String, Object>> r1Data = input1NFData.stream()
                .map(row -> {
                    Map<String, Object> newRow = new LinkedHashMap<>();
                    newRow.put(partialDeterminant, row.get(partialDeterminant));
                    newRow.put(partiallyDependentAttribute, row.get(partiallyDependentAttribute));
                    return newRow;
                })
                .distinct() // Remove redundant rows
                .collect(Collectors.toList());

        // R1 KEYS: PK is the determinant.
        List<String> r1PK = List.of(toSqlIdentifier(partialDeterminant));
        Map<String, String> r1FKs = Collections.emptyMap(); // New table has no FKs pointing out

        normalizedRelations.add(new DecomposedRelation(detailsRelationName, r1Data, r1PK, r1FKs));
        System.out.println("Decomposed Relation: " + detailsRelationName + " created.");


        // Relation 2: The residual relation (Main Relation)
        List<Map<String, Object>> residualData = input1NFData.stream()
                .map(row -> {
                    Map<String, Object> newRow = new LinkedHashMap<>(row);
                    newRow.remove(partiallyDependentAttribute); // Eliminate redundancy
                    return newRow;
                })
                .collect(Collectors.toList());

        // R2 KEYS: PK is the original composite key.
        List<String> r2PK = candidateKey.stream().map(this::toSqlIdentifier).collect(Collectors.toList());

        // The determinant column of the partial dependency becomes a Foreign Key in the Main Relation,
        // referencing the new Details relation.
        Map<String, String> r2FKs = Map.of(
                toSqlIdentifier(partialDeterminant),
                toSqlIdentifier(detailsRelationName) + "(" + toSqlIdentifier(partialDeterminant) + ")"
        );

        normalizedRelations.add(new DecomposedRelation(MAIN_RELATION_NAME, residualData, r2PK, r2FKs));

        return normalizedRelations;
    }

    /**
     * Simplified heuristic to detect partial dependency for the purpose of demonstrating decomposition.
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
        return true;
    }
}
