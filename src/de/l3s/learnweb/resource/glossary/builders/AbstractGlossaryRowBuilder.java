package de.l3s.learnweb.resource.glossary.builders;

import de.l3s.learnweb.beans.UtilBean;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;

import java.util.*;

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

    public static void main(String[] args)
    {
        Locale[] locales = { Locale.GERMAN, Locale.ENGLISH, Locale.ITALIAN, new Locale("pt") };

        for(Locale locale : locales)
            System.out.println(UtilBean.getLocaleMessage(locale, "Glossary.topic"));
    }

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
            if(isEqualForSomeLocale(getStringValueForCell(header.getCell(cellPosition)), "Glossary.first_topic"))
            {
                topicOneHeaderPosition = cellPosition;
            }
            else if(isEqualForSomeLocale(getStringValueForCell(header.getCell(cellPosition)), "Glossary.second_topic"))
            {
                topicTwoHeaderPosition = cellPosition;
            }
            else if(isEqualForSomeLocale(getStringValueForCell(header.getCell(cellPosition)), "Glossary.third_topic"))
            {
                topicThreeHeaderPosition = cellPosition;
            }
            else if(isEqualForSomeLocale(getStringValueForCell(header.getCell(cellPosition)), "description"))
            {
                descriptionHeaderPosition = cellPosition;
            }
            else if(isEqualForSomeLocale(getStringValueForCell(header.getCell(cellPosition)), "Glossary.term"))
            {
                termHeaderPosition = cellPosition;
            }
            else if(isEqualForSomeLocale(getStringValueForCell(header.getCell(cellPosition)), "language"))
            {
                languageHeaderPosition = cellPosition;
            }
            else if(isEqualForSomeLocale(getStringValueForCell(header.getCell(cellPosition)), "Glossary.use"))
            {
                usesHeaderPosition = cellPosition;
            }
            else if(isEqualForSomeLocale(getStringValueForCell(header.getCell(cellPosition)), "Glossary.pronounciation"))
            {
                pronunciationHeaderPosition = cellPosition;
            }
            else if(isEqualForSomeLocale(getStringValueForCell(header.getCell(cellPosition)), "Glossary.acronym"))
            {
                acronymHeaderPosition = cellPosition;
            }
            else if(isEqualForSomeLocale(getStringValueForCell(header.getCell(cellPosition)), "source"))
            {
                sourceHeaderPosition = cellPosition;
            }
            else if(isEqualForSomeLocale(getStringValueForCell(header.getCell(cellPosition)), "Glossary.phraseology"))
            {
                phraseologyHeaderPosition = cellPosition;
            }
            else
            {
                errors.add(new IllegalArgumentException("Unknown column name: '" + getStringValueForCell(header.getCell(cellPosition)) + "' in cell " + header.getCell(cellPosition).getAddress().formatAsString()));
            }
        }
        return errors;
    }

    public abstract T build(Row row);

    private boolean isEqualForSomeLocale(String value, String propertyAlias)
    {
        boolean result = false;

        for(Locale localeToCheck : POSSIBLE_LOCALE_LIST)
        {
            if(value != null && value.equalsIgnoreCase(UtilBean.getLocaleMessage(localeToCheck, propertyAlias)))
            {
                result = true;
            }
        }
        return result;
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
