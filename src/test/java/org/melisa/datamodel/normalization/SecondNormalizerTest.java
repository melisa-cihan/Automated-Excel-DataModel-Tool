package org.melisa.datamodel.normalization;

import org.melisa.datamodel.model.DecomposedRelation;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;

import static org.junit.jupiter.api.Assertions.*;

class SecondNormalizerTest {

    @Test
    @DisplayName("Should decompose relation when a partial dependency is detected in composite key")
    void normalizeTo2NF_partialDependency() {
        // Arrange
        // TRICK 1: ORDER MATTERS for the simplified heuristic!
        // We put "Course" FIRST so it becomes the first part of the Key.
        // The SecondNormalizer checks the first key column for dependencies.

        // Row 1: Melisa takes Math
        Map<String, Object> r1 = new LinkedHashMap<>();
        r1.put("Course", "Math");       // Put Course FIRST
        r1.put("Student", "Melisa");
        r1.put("CourseFee", 500);

        // Row 2: Melisa takes Physics
        Map<String, Object> r2 = new LinkedHashMap<>();
        r2.put("Course", "Physics");
        r2.put("Student", "Melisa");
        r2.put("CourseFee", 600);

        // Row 3: John takes Math
        Map<String, Object> r3 = new LinkedHashMap<>();
        r3.put("Course", "Math");
        r3.put("Student", "John");
        r3.put("CourseFee", 500);

        // TRICK 2: BREAK [Student, CourseFee] UNIQUENESS
        // Row 4: Melisa takes Biology (Cost 500)
        // This creates a duplicate (Melisa, 500) so [Student, CourseFee] CANNOT be a key.
        Map<String, Object> r4 = new LinkedHashMap<>();
        r4.put("Course", "Biology");
        r4.put("Student", "Melisa");
        r4.put("CourseFee", 500);

        List<Map<String, Object>> inputData = List.of(r1, r2, r3, r4);
        SecondNormalizer normalizer = new SecondNormalizer();

        // Act
        List<DecomposedRelation> results = normalizer.normalizeTo2NF(inputData, "Uni");

        // Assert
        // Now it MUST pick [Course, Student] as the key and will see that Fee depends on Course.
        assertEquals(2, results.size(), "Should decompose into exactly 2 tables");

        // 1. Verify the 'Details' table (The extracted partial dependency: Course -> Fee)
        DecomposedRelation detailsTable = results.stream()
                .filter(r -> r.name().contains("DETAILS"))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Details table was not created"));

        assertTrue(detailsTable.primaryKeys().contains("COURSE"), "Course should be the PK of the extracted table");

        // Verify we extracted the fee correctly for Math
        Object fee = detailsTable.data().stream()
                .filter(row -> "Math".equals(row.get("Course")))
                .findFirst()
                .map(row -> row.get("CourseFee"))
                .orElse(null);
        assertEquals(500, fee);

        // 2. Verify the 'Main' table (Student, Course)
        DecomposedRelation mainTable = results.stream()
                .filter(r -> r.name().contains("MAINRELATION"))
                .findFirst()
                .orElseThrow();

        // The Main table should NO LONGER contain "CourseFee"
        assertFalse(mainTable.data().get(0).containsKey("CourseFee"), "Partial dependency should be removed from main table");
    }
}