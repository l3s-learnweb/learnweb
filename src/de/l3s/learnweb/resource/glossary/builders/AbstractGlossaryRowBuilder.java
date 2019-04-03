package de.l3s.learnweb.resource.glossary.builders;

import org.apache.poi.ss.usermodel.Row;

import java.util.Locale;
import java.util.Map;

public abstract class AbstractGlossaryRowBuilder<T>
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

    //Workaround for language (change to Language utils, properties or enums...)
    protected Map<String, Locale> languageMap;

    public void headerInit(Row header, Map<String, Locale> languageMap)
    {
        this.languageMap = languageMap;
        for(int cellPosition = 0; cellPosition < header.getPhysicalNumberOfCells(); ++cellPosition)
        {
            if(header.getCell(cellPosition) == null)
            {
                return;
            }
            if("Topic 1".equalsIgnoreCase(header.getCell(cellPosition).getStringCellValue()))
            {
                topicOneHeaderPosition = cellPosition;
            }
            else if("Topic 2".equalsIgnoreCase(header.getCell(cellPosition).getStringCellValue()))
            {
                topicTwoHeaderPosition = cellPosition;
            }
            else if("Topic 3".equalsIgnoreCase(header.getCell(cellPosition).getStringCellValue()))
            {
                topicThreeHeaderPosition = cellPosition;
            }
            else if("Description".equalsIgnoreCase(header.getCell(cellPosition).getStringCellValue()))
            {
                descriptionHeaderPosition = cellPosition;
            }
            else if("Term".equalsIgnoreCase(header.getCell(cellPosition).getStringCellValue()))
            {
                termHeaderPosition = cellPosition;
            }
            else if("Language".equalsIgnoreCase(header.getCell(cellPosition).getStringCellValue()))
            {
                languageHeaderPosition = cellPosition;
            }
            else if("Uses".equalsIgnoreCase(header.getCell(cellPosition).getStringCellValue()))
            {
                usesHeaderPosition = cellPosition;
            }
            else if("pronunciation".equalsIgnoreCase(header.getCell(cellPosition).getStringCellValue()))
            {
                pronunciationHeaderPosition = cellPosition;
            }
            else if("acronym".equalsIgnoreCase(header.getCell(cellPosition).getStringCellValue()))
            {
                acronymHeaderPosition = cellPosition;
            }
            else if("source".equalsIgnoreCase(header.getCell(cellPosition).getStringCellValue()))
            {
                sourceHeaderPosition = cellPosition;
            }
            else if("phraseology".equalsIgnoreCase(header.getCell(cellPosition).getStringCellValue()))
            {
                phraseologyHeaderPosition = cellPosition;
            }
            else
            {
                throw new IllegalArgumentException("Unknown column: " + header.getCell(cellPosition).getStringCellValue() + " at position " + cellPosition);
            }
        }
    }

    public abstract T build(Row row);

}
