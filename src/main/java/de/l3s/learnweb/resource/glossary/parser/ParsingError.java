package de.l3s.learnweb.resource.glossary.parser;

import java.io.Serial;
import java.io.Serializable;
import java.util.Locale;

import org.apache.poi.ss.usermodel.Cell;
import org.omnifaces.util.Faces;

import de.l3s.learnweb.i18n.MessagesBundle;

public class ParsingError implements Serializable {
    @Serial
    private static final long serialVersionUID = 4934470524700107862L;

    private final int row;
    private final String cell;
    private final String msgKey;
    private final String[] args;

    /**
     * @param msgKey message key, translated when the error is displayed
     * @param args arguments of the message
     */
    public ParsingError(int row, String cell, String msgKey, String... args) {
        this.row = row;
        this.cell = cell;
        this.msgKey = msgKey;
        this.args = args;
    }

    /**
     * Convenience method.
     */
    public ParsingError(int rowNum, Cell cell, String msgKey, String... args) {
        this(rowNum, cell == null || cell.getAddress() == null ? null : cell.getAddress().formatAsString(), msgKey, args);
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

    /**
     * @return the error message translated to the locale of the current request, or English outside a request
     */
    public String getErrorMessage() {
        return getErrorMessage(Faces.hasContext() ? Faces.getLocale() : Locale.ENGLISH);
    }

    public String getErrorMessage(Locale locale) {
        return MessagesBundle.format(locale, msgKey, (Object[]) args);
    }
}
