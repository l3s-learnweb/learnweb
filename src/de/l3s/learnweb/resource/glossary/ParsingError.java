package de.l3s.learnweb.resource.glossary;

import java.io.Serializable;

import org.apache.poi.ss.usermodel.Cell;

public class ParsingError implements Serializable
{
    private static final long serialVersionUID = 4934470524700107862L;

    private final int row;
    private final String cell;
    private final String errorMessage;

    public ParsingError(int row, String cell, String errorMessage)
    {
        super();
        this.row = row;
        this.cell = cell;
        this.errorMessage = errorMessage;
    }

    /**
     * Convenience method
     *
     * @param rowNum
     * @param cell
     * @param errorMessage
     */
    public ParsingError(int rowNum, Cell cell, String errorMessage)
    {
        this(rowNum, cell.getAddress().formatAsString(), errorMessage);
    }

    public int getRow()
    {
        return row;
    }

    public String getCell()
    {
        return cell;
    }

    public String getErrorMessage()
    {
        return errorMessage;
    }
}
