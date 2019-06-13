package de.l3s.learnweb.resource.glossary.builders;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;

import de.l3s.learnweb.beans.UtilBean;

public abstract class AbstractGlossaryRowBuilder<T>
{

    private static final List<Locale> POSSIBLE_LOCALE_LIST = Arrays.asList(Locale.ENGLISH, Locale.ITALIAN, Locale.GERMAN, new Locale("pt"));

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

    //Workaround for language (change to Language utils, properties or enums...)
    protected Map<String, Locale> languageMap;

    public List<Exception> headerInit(Row header, Map<String, Locale> languageMap)
    {
        List<Exception> errors = new ArrayList<>();
        this.languageMap = languageMap;

        for(int cellPosition = 0; cellPosition < header.getPhysicalNumberOfCells(); ++cellPosition)
        {
            if(header.getCell(cellPosition) == null)
            {
                return errors;
            }

            String cellValue = getStringValueForCell(header.getCell(cellPosition));

            if(isEqualForSomeLocale(cellValue, "Glossary.first_topic"))
            {
                topicOneHeaderPosition = cellPosition;
            }
            else if(isEqualForSomeLocale(cellValue, "Glossary.second_topic"))
            {
                topicTwoHeaderPosition = cellPosition;
            }
            else if(isEqualForSomeLocale(cellValue, "Glossary.third_topic"))
            {
                topicThreeHeaderPosition = cellPosition;
            }
            else if(isEqualForSomeLocale(cellValue, "description"))
            {
                descriptionHeaderPosition = cellPosition;
            }
            else if(isEqualForSomeLocale(cellValue, "Glossary.term"))
            {
                termHeaderPosition = cellPosition;
            }
            else if(isEqualForSomeLocale(cellValue, "language"))
            {
                languageHeaderPosition = cellPosition;
            }
            else if(isEqualForSomeLocale(cellValue, "Glossary.use"))
            {
                usesHeaderPosition = cellPosition;
            }
            else if(isEqualForSomeLocale(cellValue, "Glossary.pronounciation"))
            {
                pronunciationHeaderPosition = cellPosition;
            }
            else if(isEqualForSomeLocale(cellValue, "Glossary.acronym"))
            {
                acronymHeaderPosition = cellPosition;
            }
            else if(isEqualForSomeLocale(cellValue, "source"))
            {
                sourceHeaderPosition = cellPosition;
            }
            else if(isEqualForSomeLocale(cellValue, "Glossary.phraseology"))
            {
                phraseologyHeaderPosition = cellPosition;
            }
            else
            {
                errors.add(new IllegalArgumentException("Unknown column name: '" + cellValue + "' in cell " + header.getCell(cellPosition).getAddress().formatAsString()));
            }
        }
        return errors;
    }

    public abstract T build(Row row);

    private boolean isEqualForSomeLocale(String value, String propertyAlias)
    {
        for(Locale localeToCheck : POSSIBLE_LOCALE_LIST)
        {
            String translation = UtilBean.getLocaleMessage(localeToCheck, propertyAlias);
            System.out.println(propertyAlias + "\t" + translation);
            if(value.equalsIgnoreCase(translation))
            {
                return true;
            }
        }
        return false;
    }

    protected String getStringValueForCell(Cell cell)
    {
        String result;
        switch(cell.getCellType())
        {
        case STRING:
            result = cell.getStringCellValue();
            break;
        case NUMERIC:
            result = String.valueOf(cell.getNumericCellValue());
            break;
        case FORMULA:
            result = cell.getCellFormula();
            break;
        default:
            result = "";
        }
        return result;
    }

}
