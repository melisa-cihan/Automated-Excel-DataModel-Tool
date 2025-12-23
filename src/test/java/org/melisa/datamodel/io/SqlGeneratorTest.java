package org.melisa.datamodel.io;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;

import static org.junit.jupiter.api.Assertions.*;

class SqlGeneratorTest {

    @Test
    @DisplayName("Should infer INTEGER SQL type from a String containing a number")
    void generateSqlScript_typeInference() {
        // Arrange
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("Age", "25"); // Java String "25" -> SQL INTEGER

        List<Map<String, Object>> data = List.of(row);
        List<String> emptyKeys = Collections.emptyList();

        // Act
        String sqlScript = SqlGenerator.generateSqlScript(data, "Users", emptyKeys, Collections.emptyMap());

        // Assert
        // FIX: Expect "AGE" (uppercase) because SqlGenerator.toSqlIdentifier sanitizes names
        assertTrue(sqlScript.contains("AGE INTEGER"),
                "Should detect that '25' is an integer and use INTEGER type for column AGE");
    }

    @Test
    @DisplayName("Should generate correct PRIMARY KEY constraint syntax")
    void generateSqlScript_primaryKeyGeneration() {
        // Arrange
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("ID", 101);
        List<String> primaryKeys = List.of("ID");

        // Act
        String sqlScript = SqlGenerator.generateSqlScript(List.of(row), "TestTable", primaryKeys, Collections.emptyMap());

        // Assert
        assertTrue(sqlScript.contains("CONSTRAINT PK_TESTTABLE PRIMARY KEY (ID)"),
                "SQL should contain correctly formatted Primary Key constraint");
    }

    @Test
    @DisplayName("Should generate correct CREATE TABLE and INSERT statements")
    void generateSqlScript_completeScriptGeneration() {
        // Arrange: Create a realistic dataset with mixed types
        Map<String, Object> row1 = new LinkedHashMap<>();
        row1.put("ID", 1);
        row1.put("Name", "Laptop");
        row1.put("Price", 999.99);
        row1.put("CategoryID", 10);

        Map<String, Object> row2 = new LinkedHashMap<>();
        row2.put("ID", 2);
        row2.put("Name", "Mouse");
        row2.put("Price", 25.50);
        row2.put("CategoryID", 10);

        List<Map<String, Object>> data = List.of(row1, row2);
        List<String> primaryKeys = List.of("ID");

        // FIX: Use uppercase "CATEGORYID" to match the sanitized column name in the table
        Map<String, String> foreignKeys = Map.of("CATEGORYID", "CATEGORY(ID)");

        // Act
        String sqlScript = SqlGenerator.generateSqlScript(data, "Product", primaryKeys, foreignKeys);

        // Assert
        // 1. Verify CREATE TABLE Block (All caps expectations)
        assertTrue(sqlScript.contains("CREATE TABLE PRODUCT"), "Should contain CREATE TABLE statement");
        assertTrue(sqlScript.contains("ID INTEGER NOT NULL"), "ID should be INTEGER and NOT NULL (PK)");
        assertTrue(sqlScript.contains("NAME VARCHAR(255)"), "Name should be VARCHAR");
        assertTrue(sqlScript.contains("PRICE DECIMAL(18, 4)"), "Price should be DECIMAL");

        // 2. Verify Constraints
        assertTrue(sqlScript.contains("CONSTRAINT PK_PRODUCT PRIMARY KEY (ID)"),
                "Should define the Primary Key constraint");
        assertTrue(sqlScript.contains("CONSTRAINT FK_PRODUCT_CATEGORYID FOREIGN KEY (CATEGORYID) REFERENCES CATEGORY(ID)"),
                "Should define the Foreign Key constraint");

        // 3. Verify INSERT Statements
        assertTrue(sqlScript.contains("INSERT INTO PRODUCT"), "Should contain INSERT statements");
        // Values usually don't change case, so 'Laptop' stays 'Laptop'
        assertTrue(sqlScript.contains("VALUES (1, 'Laptop', 999.99, 10)"), "Row 1 values should be formatted correctly");
        assertTrue(sqlScript.contains("VALUES (2, 'Mouse', 25.5, 10)"), "Row 2 values should be formatted correctly");
    }
}