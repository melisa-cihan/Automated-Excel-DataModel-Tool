package org.melisa.datamodel.normalization;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Identifies all minimal Candidate Keys for a given relation (List of Maps).
 * This class ensures a strict adherence to relational theory by checking both
 * Uniqueness (Superkey) and Minimality.
 *
 * The core logic iterates through the powerset of attributes, ordered by size,
 * to efficiently identify minimal key sets.
 *
 * Time Complexity is driven by the necessity to check 2^N subsets of attributes,
 * which is inherent to the problem of dependency analysis.
 */
public class CandidateKeyIdentifier {

    /**
     * Entry point to identify all minimal Candidate Keys.
     *
     * @param data The 1NF data, where each Map is a tuple (row).
     * @return A Set of Sets of Strings, representing all minimal Candidate Keys.
     */
    public Set<Set<String>> identifyAllCandidateKeys(List<Map<String, Object>> data) {
        if (data == null || data.isEmpty()) {
            return Collections.emptySet();
        }

        Set<Set<String>> candidateKeys = new HashSet<>();
        Set<String> allAttributes = data.get(0).keySet();
        List<String> attributeList = new ArrayList<>(allAttributes);
        int n = attributeList.size();

        // Iterate through all subsets of attributes (powerset) from size 1 up to N.
        // Starting with smallest size ensures that the first discovered superkeys are automatically minimal.
        for (int size = 1; size <= n; size++) {
            // Generate all combinations of the current size (e.g., all pairs, all triplets)
            Set<Set<String>> currentSizeSubsets = generateSubsetsOfSize(attributeList, size);

            for (Set<String> subset : currentSizeSubsets) {
                // Check Minimality: If this subset is a superset of an already found Candidate Key, skip it.
                if (isSupersetOfExistingCandidateKey(subset, candidateKeys)) {
                    continue;
                }

                // Check Uniqueness: Is this subset a Superkey?
                if (isSuperKey(subset, data)) {
                    // It is a Superkey AND it is minimal (because we check smaller subsets first).
                    candidateKeys.add(subset);
                }
            }
        }

        return candidateKeys;
    }

    /**
     * Checks if the given attribute set 'keyCandidate' is a Superkey.
     * A Superkey must uniquely determine every tuple in the relation.
     *
     * @param keyCandidate The set of attributes to test.
     * @param data The input relation data.
     * @return true if the attribute set is unique across all tuples (Superkey), false otherwise.
     */
    private boolean isSuperKey(Set<String> keyCandidate, List<Map<String, Object>> data) {
        // We use a HashSet to check for key value uniqueness.
        Set<String> seenKeys = new HashSet<>();

        for (Map<String, Object> row : data) {
            // Build a composite key string from the values of the keyCandidate attributes.
            String compositeKey = keyCandidate.stream()
                    .map(row::get)
                    .map(Object::toString)
                    .collect(Collectors.joining("|")); // Use a reliable delimiter

            if (seenKeys.contains(compositeKey)) {
                // Duplicated composite key found -> not a Superkey (violates Uniqueness)
                return false;
            }
            seenKeys.add(compositeKey);
        }
        return true;
    }

    /**
     * Checks if the current subset contains any of the already confirmed minimal Candidate Keys.
     * This enforces the Minimality principle (a Superkey is a Candidate Key only if it has no
     * smaller Superkey as a subset).
     */
    private boolean isSupersetOfExistingCandidateKey(Set<String> subset, Set<Set<String>> candidateKeys) {
        for (Set<String> candidateKey : candidateKeys) {
            if (subset.containsAll(candidateKey)) {
                return true; // Subset is not minimal.
            }
        }
        return false;
    }

    /**
     * Recursive utility to generate all subsets of a specific size.
     */
    private Set<Set<String>> generateSubsetsOfSize(List<String> attributes, int size) {
        Set<Set<String>> result = new HashSet<>();
        combine(attributes, size, 0, new LinkedHashSet<>(), result);
        return result;
    }

    private void combine(List<String> attributes, int size, int start, Set<String> current, Set<Set<String>> result) {
        if (current.size() == size) {
            result.add(new LinkedHashSet<>(current));
            return;
        }
        for (int i = start; i < attributes.size(); i++) {
            current.add(attributes.get(i));
            combine(attributes, size, i + 1, current, result);
            current.remove(attributes.get(i));
        }
    }
}
