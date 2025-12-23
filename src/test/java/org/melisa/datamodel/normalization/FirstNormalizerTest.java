package org.melisa.datamodel.normalization;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

class FirstNormalizerTest {

    // --- 1. Row Splitting (Atomicity) ---

    @Test
    @DisplayName("Row Splitting: Should split comma-separated values into multiple atomic rows")
    void normalizeTo1NF_rowSplitting() {
        // Arrange
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("ID", 1);
        row.put("Colors", "Red, Blue"); // Violation of 1NF

        List<Map<String, Object>> inputData = List.of(row);

        // Act
        List<Map<String, Object>> result = FirstNormalizer.normalizeTo1NF(inputData);

        // Assert
        assertEquals(2, result.size(), "Should produce 2 rows (one for Red, one for Blue)");
        assertEquals("Red", result.get(0).get("Colors"));
        assertEquals("Blue", result.get(1).get("Colors"));
        assertEquals(1, result.get(0).get("ID"), "ID should be preserved in both rows");
    }

    // --- 2. Column Splitting Heuristics ---

    @Test
    @DisplayName("Currency Heuristic: Should split '50.00 €' into Amount and Currency columns")
    void normalizeTo1NF_currencyHeuristic() {
        // Arrange
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("Product", "Laptop");
        row.put("Price", "50.00 €"); // Target for CurrencyHeuristic

        List<Map<String, Object>> inputData = List.of(row);

        // Act
        List<Map<String, Object>> result = FirstNormalizer.normalizeTo1NF(inputData);

        // Assert
        Map<String, Object> resultRow = result.get(0);

        // Verify new columns
        assertTrue(resultRow.containsKey("Price_Amount"));
        assertTrue(resultRow.containsKey("Price_Currency"));

        // Verify values
        assertEquals(50.0, resultRow.get("Price_Amount"));
        assertEquals("€", resultRow.get("Price_Currency"));
    }

    @Test
    @DisplayName("Value-Unit Heuristic: Should split '50 kg' into Value and Unit columns")
    void normalizeTo1NF_valueUnitHeuristic() {
        // Arrange
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("Item", "Rice");
        row.put("Weight", "50 kg"); // Target for ValueUnitHeuristic

        List<Map<String, Object>> inputData = List.of(row);

        // Act
        List<Map<String, Object>> result = FirstNormalizer.normalizeTo1NF(inputData);

        // Assert
        Map<String, Object> resultRow = result.get(0);

        // Verify values
        assertEquals(50.0, resultRow.get("Weight_Value"));
        assertEquals("kg", resultRow.get("Weight_Unit"));
    }

    @Test
    @DisplayName("Quantity-Item Heuristic: Should split '5 Books' into Quantity and Item columns")
    void normalizeTo1NF_quantityItemHeuristic() {
        // Arrange
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("Inventory", "5 Books"); // Target for QuantityItemHeuristic

        List<Map<String, Object>> inputData = List.of(row);

        // Act
        List<Map<String, Object>> result = FirstNormalizer.normalizeTo1NF(inputData);

        // Assert
        Map<String, Object> resultRow = result.get(0);

        // Verify split
        assertTrue(resultRow.containsKey("Inventory_Quantity"));
        assertTrue(resultRow.containsKey("Inventory_Item"));

        assertEquals(5, resultRow.get("Inventory_Quantity")); // Should be parsed as Integer
        assertEquals("Books", resultRow.get("Inventory_Item"));
    }

    @Test
    @DisplayName("Parenthetical Alias Heuristic: Should split 'Google (Alphabet)' into Primary and Alias")
    void normalizeTo1NF_parentheticalAliasHeuristic() {
        // Arrange
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("Company", "Google (Alphabet)"); // Target for ParentheticalAliasHeuristic

        List<Map<String, Object>> inputData = List.of(row);

        // Act
        List<Map<String, Object>> result = FirstNormalizer.normalizeTo1NF(inputData);

        // Assert
        Map<String, Object> resultRow = result.get(0);

        // Verify split
        assertTrue(resultRow.containsKey("Company_Primary"));
        assertTrue(resultRow.containsKey("Company_Alias"));

        assertEquals("Google", resultRow.get("Company_Primary"));
        assertEquals("Alphabet", resultRow.get("Company_Alias"));
    }
}