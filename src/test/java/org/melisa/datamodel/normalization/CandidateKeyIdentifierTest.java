package org.melisa.datamodel.normalization;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.LinkedHashMap;

import static org.junit.jupiter.api.Assertions.*;

class CandidateKeyIdentifierTest {

    @Test
    @DisplayName("Should identify a single column as key when it is unique")
    void identifyAllCandidateKeys_simpleKey() {
        // Arrange
        // ID is unique. Name is NOT unique.
        Map<String, Object> r1 = Map.of("ID", 1, "Name", "Melisa");
        Map<String, Object> r2 = Map.of("ID", 2, "Name", "Melisa");

        List<Map<String, Object>> data = List.of(r1, r2);
        CandidateKeyIdentifier identifier = new CandidateKeyIdentifier();

        // Act
        Set<Set<String>> keys = identifier.identifyAllCandidateKeys(data);

        // Assert
        // Should find exactly 1 key: [ID]
        assertEquals(1, keys.size());
        assertTrue(keys.contains(Set.of("ID")));
    }

    @Test
    @DisplayName("Should identify composite key when no single column is unique")
    void identifyAllCandidateKeys_compositeKey() {
        // Arrange
        // "Student" repeats (Melisa, Melisa)
        // "Course" repeats (Math, Math)
        // But the COMBINATION "Student + Course" is unique.
        Map<String, Object> r1 = Map.of("Student", "Melisa", "Course", "Math");
        Map<String, Object> r2 = Map.of("Student", "Melisa", "Course", "Physics");
        Map<String, Object> r3 = Map.of("Student", "John",   "Course", "Math");

        List<Map<String, Object>> data = List.of(r1, r2, r3);
        CandidateKeyIdentifier identifier = new CandidateKeyIdentifier();

        // Act
        Set<Set<String>> keys = identifier.identifyAllCandidateKeys(data);

        // Assert
        // Should find exactly 1 key: [Student, Course]
        assertEquals(1, keys.size());
        assertTrue(keys.contains(Set.of("Student", "Course")));
    }

    @Test
    @DisplayName("Should strictly enforce Minimality (ignore Superkeys)")
    void identifyAllCandidateKeys_minimality() {
        // Arrange
        // "ID" is unique.
        // "Email" is ALSO unique.
        // Therefore, [ID, Email] is a Superkey, but NOT a Candidate Key (because it's not minimal).
        Map<String, Object> r1 = Map.of("ID", 1, "Email", "a@test.com");
        Map<String, Object> r2 = Map.of("ID", 2, "Email", "b@test.com");

        List<Map<String, Object>> data = List.of(r1, r2);
        CandidateKeyIdentifier identifier = new CandidateKeyIdentifier();

        // Act
        Set<Set<String>> keys = identifier.identifyAllCandidateKeys(data);

        // Assert
        // We expect 2 separate keys: [ID] and [Email].
        // We do NOT want [ID, Email] combined.
        assertEquals(2, keys.size());
        assertTrue(keys.contains(Set.of("ID")));
        assertTrue(keys.contains(Set.of("Email")));

        // Explicitly check that the non-minimal superkey is absent
        assertFalse(keys.contains(Set.of("ID", "Email")), "Should not include non-minimal superkeys");
    }
}