package de.l3s.learnweb.resource.glossary.builders;

import java.util.*;

import de.l3s.learnweb.Learnweb;
import de.l3s.learnweb.resource.glossary.GlossaryEntry;
import de.l3s.learnweb.resource.glossary.GlossaryTerm;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;

import de.l3s.learnweb.LanguageBundle;
import de.l3s.learnweb.beans.UtilBean;

public class GlossaryRowBuilder
{
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

    public List<ParsingError> headerInit(Row header, Map<String, Locale> languageMap)
    {
        List<ParsingError> errors = new ArrayList<>();
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
            else if(isEqualForSomeLocale(cellValue, "Glossary.description"))
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
                errors.add(new ParsingError(0, header.getCell(cellPosition).getAddress().formatAsString(),
                        "Unknown column name: " + cellValue));
            }
        }
        return errors;
    }

    private boolean isEqualForSomeLocale(String value, String propertyAlias)
    {
        for(Locale localeToCheck : LanguageBundle.getSupportedLocales())
        {
            String translation = UtilBean.getLocaleMessage(localeToCheck, propertyAlias);

            if(value.equalsIgnoreCase(translation))
            {
                return true;
            }
        }
        return false;
    }

    protected String getStringValueForCell(Cell cell)
    {
        switch(cell.getCellType())
        {
        case STRING:
            return cell.getStringCellValue();
        case NUMERIC:
            return String.valueOf(cell.getNumericCellValue());
        case FORMULA:
            return cell.getCellFormula();
        default:
            return "";
        }
    }

    public Pair<GlossaryEntry, List<ParsingError>> build(Row row)
    {
        GlossaryEntry result = new GlossaryEntry();
        Pair<GlossaryTerm, List<ParsingError>> termWithErrors = buildTerm(row);

        if(getStringValueForCell(row.getCell(topicOneHeaderPosition)) != null)
        {
            result.setTopicOne(getStringValueForCell(row.getCell(topicOneHeaderPosition)));
        }
        else
        {
            termWithErrors.getValue().add(new ParsingError(row.getRowNum(), row.getCell(topicOneHeaderPosition).getAddress().formatAsString(),
                    "Column Topic 1 is empty"));
        }
        result.setTopicTwo(getStringValueForCell(row.getCell(topicTwoHeaderPosition)));
        result.setTopicThree(getStringValueForCell(row.getCell(topicThreeHeaderPosition)));
        result.setDescription(getStringValueForCell(row.getCell(descriptionHeaderPosition)));

        result.addTerm(termWithErrors.getKey());

        return new ImmutablePair<>(result, termWithErrors.getValue());
    }

    private Pair<GlossaryTerm, List<ParsingError>> buildTerm(Row row)
    {
        GlossaryTerm term = new GlossaryTerm();
        List<ParsingError> errors = new ArrayList<>();

        term.setTerm(getStringValueForCell(row.getCell(termHeaderPosition)));
        if(!languageMap.containsKey(getStringValueForCell(row.getCell(languageHeaderPosition))))
        {
            errors.add(new ParsingError(row.getRowNum(), row.getCell(languageHeaderPosition).getAddress().formatAsString(),
                    "Invalid language '" + getStringValueForCell(row.getCell(languageHeaderPosition)) +
                            "' This glossary is limited to " + languageMap.keySet() + " entries."));
        } else {
            term.setLanguage(languageMap.get(getStringValueForCell(row.getCell(languageHeaderPosition))));
        }
        String usesString = getStringValueForCell(row.getCell(usesHeaderPosition));
        List<String> uses = Arrays.asList(usesString.split(","));
        term.setUses(uses);

        term.setPronounciation(getStringValueForCell(row.getCell(pronunciationHeaderPosition)));
        term.setAcronym(getStringValueForCell(row.getCell(acronymHeaderPosition)));
        term.setSource(getStringValueForCell(row.getCell(sourceHeaderPosition)));
        term.setPhraseology(getStringValueForCell(row.getCell(phraseologyHeaderPosition)));

        return new ImmutablePair<>(term, errors);
    }
}
