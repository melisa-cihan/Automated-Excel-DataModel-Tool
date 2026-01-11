package org.melisa.datamodel.normalization;

import org.melisa.datamodel.io.SqlGenerator;
import org.melisa.datamodel.model.DecomposedRelation;

import java.util.*;
import java.util.stream.Collectors;

/**
 * org.melisa.datamodel.normalization.SecondNormalizer is responsible for transforming a dataset from First Normal Form (1NF)
 * into Second Normal Form (2NF). This process involves identifying candidate keys,
 * analyzing functional dependencies, and decomposing the original relation to eliminate
 * partial dependencies.
 *
 * This class adheres to the Single Responsibility Principle, focusing solely on relational decomposition.
 */
public class SecondNormalizer {


    private static final String MAIN_RELATION_NAME = "MainRelation";

    /**
     * Helper to sanitize column names using the logic from org.melisa.datamodel.io.SqlGenerator.
     * We call this to ensure key names are stored in the same format org.melisa.datamodel.io.SqlGenerator will use.
     *
     * @param name The original column name.
     * @return A sanitized, SQL-safe identifier (UPPERCASE_WITH_UNDERSCORES).
     */
    private String toSqlIdentifier(String name) {

        return SqlGenerator.toSqlIdentifier(name);
    }

    /**
     * Public interface to begin the 2NF normalization process.
     *
     * @param input1NFData The list of maps representing the data that is already in 1NF.
     * @param tableNameBase The user-provided base name (e.g., "shop") used for prefixing.
     * @return A list of org.melisa.datamodel.model.DecomposedRelation objects, each containing the data and key metadata (PK/FK).
     */
    public List<DecomposedRelation> normalizeTo2NF(
            List<Map<String, Object>> input1NFData, String tableNameBase) { // <-- UPDATED SIGNATURE

        if (input1NFData == null || input1NFData.isEmpty()) {
            return Collections.emptyList();
        }


        CandidateKeyIdentifier identifier = new CandidateKeyIdentifier();
        Set<Set<String>> allCandidateKeys = identifier.identifyAllCandidateKeys(input1NFData);

        Set<String> candidateKey = selectKeyFor2NFDecomposition(allCandidateKeys);


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


        return decomposeForPartialDependencies(input1NFData, candidateKey, sqlTableNameBase);
    }

    /**
     * Selects the most appropriate Candidate Key for 2NF decomposition.
     * Prefers a composite key (size > 1) as 2NF is only relevant for them.
     */
    private Set<String> selectKeyFor2NFDecomposition(Set<Set<String>> allCandidateKeys) {
        if (allCandidateKeys.isEmpty()) {
            return Collections.emptySet();
        }


        Set<String> smallestCompositeKey = allCandidateKeys.stream()
                .filter(key -> key.size() > 1)
                .min(Comparator.comparingInt(Set::size))
                .orElse(null);

        if (smallestCompositeKey != null) {
            return smallestCompositeKey;
        }


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
            String sqlTableNameBase) {

        List<DecomposedRelation> normalizedRelations = new ArrayList<>();


        List<String> sqlCandidateKey = candidateKey.stream()
                .map(this::toSqlIdentifier)
                .collect(Collectors.toList());


        if (sqlCandidateKey.size() <= 1) {

            String relationName = sqlTableNameBase + "_" + toSqlIdentifier(MAIN_RELATION_NAME);
            normalizedRelations.add(new DecomposedRelation(relationName, input1NFData, sqlCandidateKey, Collections.emptyMap()));
            return normalizedRelations;
        }


        final String partialDeterminant = sqlCandidateKey.get(0);


        String originalDeterminantTemp = null;
        for(String key : candidateKey) {
            if(toSqlIdentifier(key).equals(partialDeterminant)) {
                originalDeterminantTemp = key;
                break;
            }
        }


        final String originalDeterminant = originalDeterminantTemp;

        if (originalDeterminant == null) {
            System.err.println("Could not find original determinant name.");

            String relationName = sqlTableNameBase + "_" + toSqlIdentifier(MAIN_RELATION_NAME);
            normalizedRelations.add(new DecomposedRelation(relationName, input1NFData, sqlCandidateKey, Collections.emptyMap()));
            return normalizedRelations;
        }



        List<String> dependentAttributes = new ArrayList<>();

        for (String column : input1NFData.get(0).keySet()) {
            if (!candidateKey.contains(column)) {

                if (isPartiallyDependent(input1NFData, originalDeterminant, column)) {
                    dependentAttributes.add(column);
                }
            }
        }



        if (dependentAttributes.isEmpty()) {

            String relationName = sqlTableNameBase + "_" + toSqlIdentifier(MAIN_RELATION_NAME);
            normalizedRelations.add(new DecomposedRelation(relationName, input1NFData, sqlCandidateKey, Collections.emptyMap()));
            return normalizedRelations;
        }




        final String detailsRelationInternalName = partialDeterminant + "_Details";

        final String sqlDetailsRelationName = toSqlIdentifier(sqlTableNameBase + "_" + detailsRelationInternalName);



        List<Map<String, Object>> r1Data = input1NFData.stream()
                .map(row -> {
                    Map<String, Object> newRow = new LinkedHashMap<>();

                    newRow.put(originalDeterminant, row.get(originalDeterminant));


                    for (String attr : dependentAttributes) {
                        newRow.put(attr, row.get(attr));
                    }
                    return newRow;
                })
                .distinct()
                .collect(Collectors.toList());


        List<String> r1PK = List.of(partialDeterminant);
        Map<String, String> r1FKs = Collections.emptyMap();


        normalizedRelations.add(new DecomposedRelation(sqlDetailsRelationName, r1Data, r1PK, r1FKs));
        System.out.println("Decomposed Relation: " + detailsRelationInternalName + " created with attributes: " + dependentAttributes);



        final String sqlMainRelationName = toSqlIdentifier(sqlTableNameBase + "_" + MAIN_RELATION_NAME);

        List<Map<String, Object>> residualData = input1NFData.stream()
                .map(row -> {
                    Map<String, Object> newRow = new LinkedHashMap<>(row);


                    for (String attr : dependentAttributes) {
                        newRow.remove(attr);
                    }
                    return newRow;
                })
                .collect(Collectors.toList());


        List<String> r2PK = sqlCandidateKey;


        Map<String, String> r2FKs = Map.of(
                partialDeterminant,
                sqlDetailsRelationName + "(" + partialDeterminant + ")"
        );


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

                if (!Objects.equals(checkMap.get(determinantValue), dependentValue)) {

                    return false;
                }
            } else {
                checkMap.put(determinantValue, dependentValue);
            }
        }

        return checkMap.keySet().stream().anyMatch(Objects::nonNull);
    }
}