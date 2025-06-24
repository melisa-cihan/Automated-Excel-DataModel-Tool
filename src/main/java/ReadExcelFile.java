import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.ss.util.CellReference;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

public class ReadExcelFile {

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
     * handling various cell types and potential issues.
     *
     * @param headerRow The first row of the sheet containing column names.
     * @return A list of column names. If a cell is truly blank or cannot yield a non-empty string,
     * a default "ColumnN" name is assigned. Otherwise, the cell's string value is used.
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

        for (int i = 0; i <= maxColNum; i++) {
            Cell cell = headerRow.getCell(i);
            String columnName = null;

            if (cell != null) {
                DataFormatter formatter = new DataFormatter();
                columnName = formatter.formatCellValue(cell);
            }


            if (columnName == null || columnName.trim().isEmpty()) {
                columnName = "Column" + (i + 1);
            } else {
                columnName = columnName.trim();
            }

            //Ensure unique column names by adding suffix if duplicate
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
     * It also attempts to infer the data type of each cell value.
     *
     * @param dataRow     The row containing the data.
     * @param columnNames The list of column names.
     * @return A map representing the row data with column names as keys and cell values as values
     * (with inferred data types).
     */
    private static Map<String, Object> getRowData(Row dataRow, List<String> columnNames) {
        Map<String, Object> rowData = new LinkedHashMap<>();
        for (int i = 0; i < columnNames.size(); i++) {
            Cell cell = dataRow.getCell(i);
            rowData.put(columnNames.get(i), getCellValue(cell));
        }
        return rowData;
    }

    /**
     * Helper method to handle numeric cell values, including date formatting.
     * @param cell The cell to read.
     * @return The cell value as an Integer, Double, or String (for dates).
     */
    private static Object getNumericCellValue(Cell cell) {
        if (DateUtil.isCellDateFormatted(cell)) {
            return cell.getLocalDateTimeCellValue().toString();
        } else {
            double numericValue = cell.getNumericCellValue();
            if (numericValue == Math.floor(numericValue) && !Double.isInfinite(numericValue)) {
                return (int) numericValue;
            } else {
                return numericValue;
            }
        }
    }

    /**
     * Helper method to handle string cell values safely.
     * @param cell The cell to read.
     * @return The trimmed string value, or null if empty/whitespace.
     */
    private static String getStringCellValue(Cell cell) {
        String value = cell.getStringCellValue();
        return (value != null) ? value.trim() : null;
    }

    /**
     * Helper method to handle boolean cell values.
     * @param cell The cell to read.
     * @return The boolean value.
     */
    private static Boolean getBooleanCellValue(Cell cell) {
        return cell.getBooleanCellValue();
    }

    /**
     * Gets the value of a cell and attempts to determine its data type.
     *
     * @param cell The cell to read.
     * @return The cell value as a String, Integer, Double, Boolean, or LocalDateTime (as String), or null.
     */
    private static Object getCellValue(Cell cell) {
        if (cell == null) {
            return null;
        }

        switch (cell.getCellType()) {
            case STRING:
                return getStringCellValue(cell);
            case NUMERIC:
                return getNumericCellValue(cell);
            case BOOLEAN:
                return getBooleanCellValue(cell);
            case FORMULA:

                switch (cell.getCachedFormulaResultType()) {
                    case STRING:
                        return getStringCellValue(cell);
                    case NUMERIC:
                        return getNumericCellValue(cell);
                    case BOOLEAN:
                        return getBooleanCellValue(cell);
                    case ERROR:
                    case BLANK:
                        return null;
                    default:

                        try {
                            return cell.getStringCellValue().trim();
                        } catch (Exception e) {
                            return null;
                        }
                }
            case BLANK:
            case ERROR:
            default:
                return null;
        }
    }
}