package org.melisa.datamodel.io;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.usermodel.WorkbookFactory;
// import org.apache.poi.ss.util.CellReference; // Not directly used in the provided code, can remove if not used elsewhere

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
// import java.time.LocalDateTime; // Not directly used in the final version of getCellValueAsString

public class ReadExcelFile {

    // DataFormatter instance to convert cell values to their formatted string representation.
    // This is crucial for correctly reading currency symbols (€, $), units (kg, pcs), etc.,
    // that might be part of a numeric cell's display format in Excel.
    private static final DataFormatter DATA_FORMATTER = new DataFormatter();

    /**
     * Reads data from the first sheet of an Excel file into a list of maps.
     *
     * @param inputStream The InputStream of the Excel file.
     * @return A list of maps, where each map represents a row and keys are column names.
     * @throws IOException              If an error occurs while reading the file.
     * @throws IllegalArgumentException If the file format is invalid or no header row is found.
     */
    public static List<Map<String, Object>> readExcelData(InputStream inputStream) throws IOException {
        List<Map<String, Object>> data = new ArrayList<>();

        try (Workbook workbook = WorkbookFactory.create(inputStream)) {
            Sheet sheet = workbook.getSheetAt(0);

            Row headerRow = sheet.getRow(0);
            if (headerRow == null) {
                throw new IllegalArgumentException("No header row found in the Excel sheet (expected at row 0).");
            }

            int lastRow = sheet.getLastRowNum();

            List<String> columnNames = getColumnNames(headerRow);

            for (int i = 1; i <= lastRow; i++) {
                Row dataRow = sheet.getRow(i);
                if (dataRow != null) {
                    data.add(getRowData(dataRow, columnNames));
                }
            }
        }
        return data;
    }

    /**
     * Extracts column names from the header row.
     * This version is more robust in extracting string values from cells,
     * handling various cell types and potential issues. It uses DataFormatter
     * to get the exact displayed string value of header cells.
     *
     * @param headerRow The first row of the sheet containing column names.
     * @return A list of column names. If a cell is truly blank or cannot yield a non-empty string,
     * a default "ColumnN" name is assigned. Otherwise, the cell's trimmed string value is used.
     * Ensures uniqueness by adding suffixes if duplicates are found.
     */
    private static List<String> getColumnNames(Row headerRow) {
        List<String> columnNames = new ArrayList<>();

        int maxColNum = -1;
        if (headerRow != null) {
            for (Cell cell : headerRow) {
                if (cell.getColumnIndex() > maxColNum) {
                    maxColNum = cell.getColumnIndex();
                }
            }
        }

        // Iterate up to maxColNum to ensure all columns (even empty ones in the middle) are considered
        for (int i = 0; i <= maxColNum; i++) {
            Cell cell = headerRow.getCell(i);
            String rawColumnName = null;

            if (cell != null) {
                // Use the static DATA_FORMATTER to get the displayed cell value as a string
                rawColumnName = DATA_FORMATTER.formatCellValue(cell);
            }

            String columnName;
            if (rawColumnName == null || rawColumnName.trim().isEmpty()) {
                columnName = "Column" + (i + 1); // Assign default name for blank/empty headers
            } else {
                columnName = rawColumnName.trim();
            }

            // Ensure unique column names by adding a suffix if a duplicate is found
            String originalColumnName = columnName;
            int counter = 1;
            while (columnNames.contains(columnName)) {
                columnName = originalColumnName + "_" + counter++;
            }
            columnNames.add(columnName);
        }
        return columnNames;
    }

    /**
     * Reads data from a row and creates a map with column names as keys.
     * All cell values are initially read as their formatted String representation,
     * allowing subsequent heuristic processing in the org.melisa.datamodel.normalization.Normalizer.
     *
     * @param dataRow     The row containing the data.
     * @param columnNames The list of column names.
     * @return A map representing the row data with column names as keys and cell values as values
     * (all initially as Strings, or null for blank cells).
     */
    private static Map<String, Object> getRowData(Row dataRow, List<String> columnNames) {
        Map<String, Object> rowData = new LinkedHashMap<>();
        for (int i = 0; i < columnNames.size(); i++) {
            Cell cell = dataRow.getCell(i);
            // All cell values are now passed as their formatted String representation to the org.melisa.datamodel.normalization.Normalizer
            rowData.put(columnNames.get(i), getCellValueAsFormattedString(cell));
        }
        return rowData;
    }

    /**
     * Gets the formatted string value of a cell using DataFormatter.
     * This is the primary method for reading cell content, ensuring that
     * formatted numbers (like "50 €"), dates, or formulas are read as strings
     * that include their display formatting, which is crucial for heuristic processing.
     *
     * @param cell The cell to read.
     * @return The cell's formatted display value as a String, or null if the cell is truly blank/null
     * or contains only whitespace.
     */
    private static String getCellValueAsFormattedString(Cell cell) {
        if (cell == null) {
            return null;
        }

        // DataFormatter.formatCellValue() is robust:
        // - For STRING cells, it returns the string content.
        // - For NUMERIC cells (including dates), it returns the formatted number (e.g., "50 €", "01/01/2023").
        // - For FORMULA cells, it evaluates the formula and returns the formatted result.
        // - For BOOLEAN cells, it returns "TRUE" or "FALSE".
        // This ensures the org.melisa.datamodel.normalization.Normalizer receives the cell content exactly as displayed in Excel.
        String cellValue = DATA_FORMATTER.formatCellValue(cell);

        // Trim the result and return null if it's empty after trimming (i.e., was blank or just whitespace)
        return (cellValue != null && !cellValue.trim().isEmpty()) ? cellValue.trim() : null;
    }

}