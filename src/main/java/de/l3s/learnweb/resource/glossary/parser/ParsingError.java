package de.l3s.learnweb.resource.glossary.parser;

import java.io.Serial;
import java.io.Serializable;

import org.apache.poi.ss.usermodel.Cell;

public class ParsingError implements Serializable {
    @Serial
    private static final long serialVersionUID = 4934470524700107862L;

    private final int row;
    private final String cell;
    private final String errorMessage;

    public ParsingError(int row, String cell, String errorMessage) {
        this.row = row;
        this.cell = cell;
        this.errorMessage = errorMessage;
    }

    /**
     * Convenience method.
     */
    public ParsingError(int rowNum, Cell cell, String errorMessage) {
        this(rowNum, cell == null || cell.getAddress() == null ? null : cell.getAddress().formatAsString(), errorMessage);
    }

    public int getRow() {
        return row;
    }

    public String getCell() {
        return cell;
    }

    public String getRowName() {
        if (row == -1) {
            return "";
        }

        return Integer.toString(row + 1); // internal row count starts at zero
    }

    public String getErrorMessage() {
        return errorMessage;
    }
}
