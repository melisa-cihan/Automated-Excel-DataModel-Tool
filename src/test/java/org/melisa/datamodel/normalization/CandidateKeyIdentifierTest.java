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
        Map<String, Object> r1 = Map.of("ID", 1, "Name", "Melisa");
        Map<String, Object> r2 = Map.of("ID", 2, "Name", "Melisa");

        List<Map<String, Object>> data = List.of(r1, r2);
        CandidateKeyIdentifier identifier = new CandidateKeyIdentifier();

        // Act
        Set<Set<String>> keys = identifier.identifyAllCandidateKeys(data);

        // Assert
        assertEquals(1, keys.size());
        assertTrue(keys.contains(Set.of("ID")));
    }

    @Test
    @DisplayName("Should identify composite key when no single column is unique")
    void identifyAllCandidateKeys_compositeKey() {
        // Arrange
        Map<String, Object> r1 = Map.of("Student", "Melisa", "Course", "Math");
        Map<String, Object> r2 = Map.of("Student", "Melisa", "Course", "Physics");
        Map<String, Object> r3 = Map.of("Student", "John",   "Course", "Math");

        List<Map<String, Object>> data = List.of(r1, r2, r3);
        CandidateKeyIdentifier identifier = new CandidateKeyIdentifier();

        // Act
        Set<Set<String>> keys = identifier.identifyAllCandidateKeys(data);

        // Assert
        assertEquals(1, keys.size());
        assertTrue(keys.contains(Set.of("Student", "Course")));
    }

    @Test
    @DisplayName("Should strictly enforce Minimality (ignore Superkeys)")
    void identifyAllCandidateKeys_minimality() {
        // Arrange
        Map<String, Object> r1 = Map.of("ID", 1, "Email", "a@test.com");
        Map<String, Object> r2 = Map.of("ID", 2, "Email", "b@test.com");

        List<Map<String, Object>> data = List.of(r1, r2);
        CandidateKeyIdentifier identifier = new CandidateKeyIdentifier();

        // Act
        Set<Set<String>> keys = identifier.identifyAllCandidateKeys(data);

        // Assert
        assertEquals(2, keys.size());
        assertTrue(keys.contains(Set.of("ID")));
        assertTrue(keys.contains(Set.of("Email")));

        assertFalse(keys.contains(Set.of("ID", "Email")), "Should not include non-minimal superkeys");
    }
}