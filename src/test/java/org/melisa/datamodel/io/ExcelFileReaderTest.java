package org.melisa.datamodel.io;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ExcelFileReaderTest {

    @Test
    @DisplayName("Should correctly read data and handle duplicate column names from an in-memory Excel file")
    void readExcelData_integrationTest() throws IOException {
        // --- ARRANGE: Create a "Virtual" Excel file in memory ---
        // We use XSSFWorkbook to create a real Excel structure object
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("TestSheet");

            // 1. Create Header Row (Row 0)
            // Scenario: We intentionally add a duplicate column "Name" to test your renaming logic.
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("ID");
            header.createCell(1).setCellValue("Name");
            header.createCell(2).setCellValue("Name"); // Duplicate! Should become "Name_1"
            header.createCell(3).setCellValue("Price");

            // 2. Create Data Row (Row 1)
            Row dataRow = sheet.createRow(1);
            dataRow.createCell(0).setCellValue(101);      // Numeric
            dataRow.createCell(1).setCellValue("Melisa"); // String
            dataRow.createCell(2).setCellValue("Test");   // String (for the duplicate column)
            dataRow.createCell(3).setCellValue(99.99);    // Numeric (Price)

            // 3. Write this workbook to a memory stream (instead of a file on disk)
            ByteArrayOutputStream outStream = new ByteArrayOutputStream();
            workbook.write(outStream);

            // Convert it to an Input Stream that your reader expects
            ByteArrayInputStream inputStream = new ByteArrayInputStream(outStream.toByteArray());

            // --- ACT: Call your actual application code ---
            List<Map<String, Object>> result = ExcelFileReader.readExcelData(inputStream);

            // --- ASSERT: Verify the results ---
            assertNotNull(result, "Result should not be null");
            assertEquals(1, result.size(), "Should have read exactly 1 row of data");

            Map<String, Object> firstRow = result.get(0);

            // Check 1: Did it read the standard columns?
            // Note: Your ExcelFileReader reads everything as formatted Strings initially.
            // Apache POI default formatting for 101 might be "101" or "101.0" depending on cell type,
            // but usually setCellValue(int) creates a numeric cell.
            // Your reader uses DataFormatter, which handles this gracefully.
            assertEquals("101", firstRow.get("ID"));
            assertEquals("Melisa", firstRow.get("Name"));
            assertEquals("99.99", firstRow.get("Price"));

            // Check 2: Did it handle the duplicate column name?
            // The logic in ExcelFileReader should rename the second "Name" to "Name_1"
            assertTrue(firstRow.containsKey("Name_1"),
                    "The second 'Name' column should have been renamed to 'Name_1' to avoid collision");
            assertEquals("Test", firstRow.get("Name_1"));
        }
    }
}