package de.l3s.learnweb.resource.glossary.parser;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;

import de.l3s.learnweb.LanguageBundle;
import de.l3s.learnweb.resource.glossary.Column;
import de.l3s.learnweb.resource.glossary.GlossaryEntry;
import de.l3s.learnweb.resource.glossary.GlossaryTerm;
import de.l3s.util.bean.BeanHelper;

public class GlossaryRowBuilder {
    protected int topicOneHeaderPosition = -1;
    protected int topicTwoHeaderPosition = -1;
    protected int topicThreeHeaderPosition = -1;
    protected int descriptionHeaderPosition = -1;

    protected int termHeaderPosition = -1;
    protected int languageHeaderPosition = -1;
    protected int usesHeaderPosition = -1;
    protected int pronunciationHeaderPosition = -1;
    protected int acronymHeaderPosition = -1;
    protected int sourceHeaderPosition = -1;
    protected int phraseologyHeaderPosition = -1;

    protected Map<String, Locale> languageMap;

    private final List<ParsingError> errors = new ArrayList<>();

    /**
     * Checks the position and validity of the header columns.
     *
     * @param header row containing the headers
     * @return false if header could not be processed
     */
    public boolean headerInit(Row header, Map<String, Locale> languageMap) {
        this.languageMap = languageMap;

        for (int cellPosition = 0; cellPosition < header.getPhysicalNumberOfCells(); ++cellPosition) {
            if (header.getCell(cellPosition) == null) {
                errors.add(new ParsingError(header.getRowNum(), header.getCell(cellPosition), "Is null"));
                return false;
            }

            String cellValue = getStringValueForCell(header.getCell(cellPosition));

            if (isEqualForAnyLocale(cellValue, Column.topicOne)) {
                topicOneHeaderPosition = cellPosition;
            } else if (isEqualForAnyLocale(cellValue, Column.topicTwo)) {
                topicTwoHeaderPosition = cellPosition;
            } else if (isEqualForAnyLocale(cellValue, Column.topicThree)) {
                topicThreeHeaderPosition = cellPosition;
            } else if (isEqualForAnyLocale(cellValue, Column.description)) {
                descriptionHeaderPosition = cellPosition;
            } else if (isEqualForAnyLocale(cellValue, Column.term)) {
                termHeaderPosition = cellPosition;
            } else if (isEqualForAnyLocale(cellValue, Column.language)) {
                languageHeaderPosition = cellPosition;
            } else if (isEqualForAnyLocale(cellValue, Column.uses)) {
                usesHeaderPosition = cellPosition;
            } else if (isEqualForAnyLocale(cellValue, Column.pronounciation)) {
                pronunciationHeaderPosition = cellPosition;
            } else if (isEqualForAnyLocale(cellValue, Column.acronym)) {
                acronymHeaderPosition = cellPosition;
            } else if (isEqualForAnyLocale(cellValue, Column.source)) {
                sourceHeaderPosition = cellPosition;
            } else if (isEqualForAnyLocale(cellValue, Column.phraseology)) {
                phraseologyHeaderPosition = cellPosition;
            } else {
                errors.add(new ParsingError(header.getRowNum(), header.getCell(cellPosition), "Unknown column name: " + cellValue));
            }
        }
        return errors.isEmpty();
    }

    /**
     * Check if the given value is equal to any translated name of the given column.
     */
    private boolean isEqualForAnyLocale(String value, Column column) {
        return isEqualForAnyLocale(value, column.getMsgKey());
    }

    /**
     * True if the given value is equal to any translation of the given msg key.
     */
    static boolean isEqualForAnyLocale(String value, String msgKey) {
        for (Locale localeToCheck : BeanHelper.getSupportedLocales()) {
            String translation = LanguageBundle.getBundle(localeToCheck).getString(msgKey);

            if (value.equalsIgnoreCase(translation)) {
                return true;
            }
        }
        return false;
    }

    static String getStringValueForCell(Cell cell) {
        if (cell == null) {
            return "";
        }
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue().trim();
            case NUMERIC -> String.valueOf(cell.getNumericCellValue());
            case FORMULA -> cell.getCellFormula().trim();
            default -> "";
        };
    }

    public GlossaryEntry build(Row row) {
        GlossaryEntry result = new GlossaryEntry();
        GlossaryTerm term = buildTerm(row);

        if (getStringValueForCell(row.getCell(topicOneHeaderPosition)) != null) {
            result.setTopicOne(getStringValueForCell(row.getCell(topicOneHeaderPosition)));
        } else {
            errors.add(new ParsingError(row.getRowNum(), row.getCell(topicOneHeaderPosition), "Column Topic 1 is empty"));
        }
        result.setTopicTwo(getStringValueForCell(row.getCell(topicTwoHeaderPosition)));
        result.setTopicThree(getStringValueForCell(row.getCell(topicThreeHeaderPosition)));
        result.setDescription(getStringValueForCell(row.getCell(descriptionHeaderPosition)));

        result.addTerm(term);

        return result;
    }

    private GlossaryTerm buildTerm(Row row) {
        GlossaryTerm term = new GlossaryTerm();

        String cellValue = getStringValueForCell(row.getCell(languageHeaderPosition));

        term.setTerm(getStringValueForCell(row.getCell(termHeaderPosition)));
        if (languageMap.containsKey(cellValue)) {
            term.setLanguage(languageMap.get(cellValue));
        } else {

            errors.add(new ParsingError(row.getRowNum(), row.getCell(languageHeaderPosition),
                "Invalid language; Current value: " + StringUtils.firstNonBlank(cellValue, "<i>empty</i>") +
                    "; Valid values: " + String.join(", ", languageMap.keySet())));
        }
        String usesString = getStringValueForCell(row.getCell(usesHeaderPosition));
        List<String> uses = Arrays.asList(usesString.split(","));
        term.setUses(uses);

        term.setPronounciation(getStringValueForCell(row.getCell(pronunciationHeaderPosition)));
        term.setAcronym(getStringValueForCell(row.getCell(acronymHeaderPosition)));
        term.setSource(getStringValueForCell(row.getCell(sourceHeaderPosition)));
        term.setPhraseology(getStringValueForCell(row.getCell(phraseologyHeaderPosition)));

        return term;
    }

    public List<ParsingError> getErrors() {
        return errors;
    }

}
