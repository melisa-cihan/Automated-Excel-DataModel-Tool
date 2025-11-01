import java.util.*;
import java.util.stream.Collectors;

/**
 * SecondNormalizer is responsible for transforming a dataset from First Normal Form (1NF)
 * into Second Normal Form (2NF). This process involves identifying candidate keys,
 * analyzing functional dependencies, and decomposing the original relation to eliminate
 * partial dependencies.
 *
 * This class adheres to the Single Responsibility Principle, focusing solely on relational decomposition.
 */
public class SecondNormalizer {

    // The internal, non-prefixed name for the main relation.
    private static final String MAIN_RELATION_NAME = "MainRelation";

    /**
     * Helper to sanitize column names using the logic from SqlGenerator.
     * We call this to ensure key names are stored in the same format SqlGenerator will use.
     *
     * @param name The original column name.
     * @return A sanitized, SQL-safe identifier (UPPERCASE_WITH_UNDERSCORES).
     */
    private String toSqlIdentifier(String name) {
        // Since SqlGenerator is in the same package, we can call its public static method directly.
        // This is cleaner and safer than reflection.
        return SqlGenerator.toSqlIdentifier(name);
    }

    /**
     * Public interface to begin the 2NF normalization process.
     *
     * @param input1NFData The list of maps representing the data that is already in 1NF.
     * @param tableNameBase The user-provided base name (e.g., "shop") used for prefixing.
     * @return A list of DecomposedRelation objects, each containing the data and key metadata (PK/FK).
     */
    public List<DecomposedRelation> normalizeTo2NF(
            List<Map<String, Object>> input1NFData, String tableNameBase) { // <-- UPDATED SIGNATURE

        if (input1NFData == null || input1NFData.isEmpty()) {
            return Collections.emptyList();
        }

        // Step 1: Synthesize the Primary/Candidate Key.
        CandidateKeyIdentifier identifier = new CandidateKeyIdentifier();
        Set<Set<String>> allCandidateKeys = identifier.identifyAllCandidateKeys(input1NFData);

        Set<String> candidateKey = selectKeyFor2NFDecomposition(allCandidateKeys);

        // Sanitize the user's base name *once* for use in all relation names.
        final String sqlTableNameBase = toSqlIdentifier(tableNameBase);

        if (candidateKey.isEmpty()) {
            System.err.println("Error: No Candidate Key could be identified for the relation. Returning original data as MainRelation.");
            List<String> emptyKeys = Collections.emptyList();
            Map<String, String> emptyFks = Collections.emptyMap();
            // Use the fully prefixed, sanitized name
            String relationName = sqlTableNameBase + "_" + toSqlIdentifier(MAIN_RELATION_NAME);
            return List.of(new DecomposedRelation(relationName, input1NFData, emptyKeys, emptyFks));
        }

        System.out.println("Selected Candidate Key for 2NF: " + candidateKey);

        // Step 2: Identify and Decompose Partial Dependencies.
        // Pass the base name down so the decomposer can build correct FK references.
        return decomposeForPartialDependencies(input1NFData, candidateKey, sqlTableNameBase); // <-- UPDATED CALL
    }

    /**
     * Selects the most appropriate Candidate Key for 2NF decomposition.
     * Prefers a composite key (size > 1) as 2NF is only relevant for them.
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
     * @param sqlTableNameBase The SQL-sanitized base name (e.g., "SHOP") to build FK references.
     * @return A list of new, decomposed relations with metadata.
     */
    private List<DecomposedRelation> decomposeForPartialDependencies(
            List<Map<String, Object>> input1NFData,
            Set<String> candidateKey,
            String sqlTableNameBase) { // <-- UPDATED SIGNATURE

        List<DecomposedRelation> normalizedRelations = new ArrayList<>();

        // Sanitize the candidate key column names *once* for consistency.
        List<String> sqlCandidateKey = candidateKey.stream()
                .map(this::toSqlIdentifier)
                .collect(Collectors.toList());

        // If the key is not composite (single column), the relation is automatically in 2NF.
        if (sqlCandidateKey.size() <= 1) {
            // *** UPDATE: Use prefixed name for the relation ***
            String relationName = sqlTableNameBase + "_" + toSqlIdentifier(MAIN_RELATION_NAME);
            normalizedRelations.add(new DecomposedRelation(relationName, input1NFData, sqlCandidateKey, Collections.emptyMap()));
            return normalizedRelations;
        }

        // --- SIMPLIFIED 2NF HEURISTIC ---
        // We select the first column of the composite key as the determinant.
        final String partialDeterminant = sqlCandidateKey.get(0);

        // Find the original (non-sanitized) name for the determinant
        String originalDeterminantTemp = null; // Use a temporary variable
        for(String key : candidateKey) {
            if(toSqlIdentifier(key).equals(partialDeterminant)) {
                originalDeterminantTemp = key;
                break;
            }
        }

        // *** FIX: Assign to a final variable ***
        final String originalDeterminant = originalDeterminantTemp;

        if (originalDeterminant == null) {
            System.err.println("Could not find original determinant name.");
            // Handle error: return non-decomposed data
            // *** UPDATE: Use prefixed name for the relation ***
            String relationName = sqlTableNameBase + "_" + toSqlIdentifier(MAIN_RELATION_NAME);
            normalizedRelations.add(new DecomposedRelation(relationName, input1NFData, sqlCandidateKey, Collections.emptyMap()));
            return normalizedRelations;
        }


        String dependentAttrTemp = null;
        for (String column : input1NFData.get(0).keySet()) {
            if (!candidateKey.contains(column)) { // Check against original, non-sanitized key set
                // Check if it's determined only by the first part of the key
                if (isPartiallyDependent(input1NFData, originalDeterminant, column)) {
                    dependentAttrTemp = column;
                    break;
                }
            }
        }

        // *** FIX: Assign to a final variable ***
        final String partiallyDependentAttribute = dependentAttrTemp;

        if (partiallyDependentAttribute == null) {
            // No partial dependencies found/simulated, relation is 2NF.
            // *** UPDATE: Use prefixed name for the relation ***
            String relationName = sqlTableNameBase + "_" + toSqlIdentifier(MAIN_RELATION_NAME);
            normalizedRelations.add(new DecomposedRelation(relationName, input1NFData, sqlCandidateKey, Collections.emptyMap()));
            return normalizedRelations;
        }

        // --- DECOMPOSITION EXECUTION & KEY ASSIGNMENT ---

        // Relation 1: The partially dependent relation (e.g., ProNr_Details or Worker_Details)
        final String detailsRelationInternalName = partialDeterminant + "_Details";
        // *** FIX: Create the full, prefixed SQL name ***
        final String sqlDetailsRelationName = toSqlIdentifier(sqlTableNameBase + "_" + detailsRelationInternalName);
        final String sqlPartiallyDependentAttribute = toSqlIdentifier(partiallyDependentAttribute);

        // Find the original (non-sanitized) name for the dependent attribute
        String originalDependentAttributeTemp = null; // Use a temporary variable
        for(String key : input1NFData.get(0).keySet()) {
            if(toSqlIdentifier(key).equals(sqlPartiallyDependentAttribute)) {
                originalDependentAttributeTemp = key;
                break;
            }
        }

        // *** FIX: Assign to a final variable ***
        final String originalDependentAttribute = originalDependentAttributeTemp;

        // *** UPDATED BLOCK: Add explicit error handling ***
        if (originalDependentAttribute == null) {
            // This indicates a critical internal error if it ever happens.
            System.err.println("Critical Error: Could not map sanitized dependent attribute back to original.");
            // Abort decomposition and return the original 2NF data safely.
            String relationName = sqlTableNameBase + "_" + toSqlIdentifier(MAIN_RELATION_NAME);
            normalizedRelations.add(new DecomposedRelation(relationName, input1NFData, sqlCandidateKey, Collections.emptyMap()));
            return normalizedRelations;
        }


        List<Map<String, Object>> r1Data = input1NFData.stream()
                .map(row -> {
                    Map<String, Object> newRow = new LinkedHashMap<>();
                    // *** FIX: Use final variables inside lambda ***
                    newRow.put(originalDeterminant, row.get(originalDeterminant));
                    newRow.put(originalDependentAttribute, row.get(originalDependentAttribute));
                    return newRow;
                })
                .distinct() // Remove redundant rows
                .collect(Collectors.toList());

        // R1 KEYS: PK is the determinant.
        List<String> r1PK = List.of(partialDeterminant); // Already sanitized
        Map<String, String> r1FKs = Collections.emptyMap();

        // *** UPDATE: Use the full prefixed SQL name ***
        normalizedRelations.add(new DecomposedRelation(sqlDetailsRelationName, r1Data, r1PK, r1FKs));
        System.out.println("Decomposed Relation: " + detailsRelationInternalName + " created.");


        // Relation 2: The residual relation (Main Relation)
        // *** UPDATE: Use the full prefixed SQL name ***
        final String sqlMainRelationName = toSqlIdentifier(sqlTableNameBase + "_" + MAIN_RELATION_NAME);

        List<Map<String, Object>> residualData = input1NFData.stream()
                .map(row -> {
                    Map<String, Object> newRow = new LinkedHashMap<>(row);
                    // *** FIX: Use final variable inside lambda ***
                    newRow.remove(originalDependentAttribute); // Eliminate redundancy
                    return newRow;
                })
                .collect(Collectors.toList());

        // R2 KEYS: PK is the original composite key.
        List<String> r2PK = sqlCandidateKey; // Already sanitized

        // *** THE FIX IS HERE ***
        // Build the full, correct reference string using the prefixed name.
        Map<String, String> r2FKs = Map.of(
                partialDeterminant, // The FK column (e.g., "SIZE")
                sqlDetailsRelationName + "(" + partialDeterminant + ")" // The reference (e.g., "SHOP_SIZE_DETAILS(SIZE)")
        );

        // *** UPDATE: Use the full prefixed SQL name ***
        normalizedRelations.add(new DecomposedRelation(sqlMainRelationName, residualData, r2PK, r2FKs));

        return normalizedRelations;
    }

    /**
     * Simplified heuristic to detect partial dependency.
     * Checks if attribute 'dependentAttr' is functionally dependent on 'determinant'.
     */
    private boolean isPartiallyDependent(List<Map<String, Object>> data, String determinant, String dependentAttr) {
        Map<Object, Object> checkMap = new HashMap<>();

        for (Map<String, Object> row : data) {
            Object determinantValue = row.get(determinant);
            Object dependentValue = row.get(dependentAttr);

            if (checkMap.containsKey(determinantValue)) {
                // If a determinant value already exists, check if the dependent value is consistent
                // *** FIX: Use Objects.equals() to safely handle potential null values ***
                if (!Objects.equals(checkMap.get(determinantValue), dependentValue)) {
                    // Inconsistency found: Not a functional dependency.
                    return false;
                }
            } else {
                checkMap.put(determinantValue, dependentValue);
            }
        }
        // If the loop completes without inconsistencies, it's a functional dependency.
        // We also check that the determinant wasn't just null for the whole table.
        return checkMap.keySet().stream().anyMatch(Objects::nonNull);
    }
}

