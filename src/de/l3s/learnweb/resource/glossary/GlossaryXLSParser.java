package de.l3s.learnweb.resource.glossary;

import de.l3s.learnweb.resource.glossary.builders.GlossaryEntryGlossaryRowBuilder;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;
import org.apache.poi.ss.usermodel.Row;
import org.primefaces.model.UploadedFile;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class GlossaryXLSParser
{
    private static final Logger log = Logger.getLogger(GlossaryXLSParser.class);

    private List<GlossaryEntry> sortedGlossaryEntries = new ArrayList<>();
    private UploadedFile uploadedFile;
    private Map<String, Locale> languageMap;
    private List<Exception> errorsDuringProcessing = new ArrayList<>();


    public List<Exception> getErrorsDuringProcessing()
    {
        return Collections.unmodifiableList(errorsDuringProcessing);
    }

    public GlossaryXLSParser(UploadedFile uploadedFile, Map<String, Locale> languageMap)
    {
        this.uploadedFile = uploadedFile;
        this.languageMap = languageMap;
    }

    private boolean isEmptyRow(Row row)
    {
        if(row != null)
        {
            for(int cellNumber = 0; cellNumber < row.getPhysicalNumberOfCells(); ++cellNumber)
            {
                if(StringUtils.isNoneEmpty(row.getCell(cellNumber).getStringCellValue()))
                {
                    return false;
                }
            }
        }
        return true;
    }

    public void parseGlossaryEntries() throws IOException
    {
        POIFSFileSystem fs = new POIFSFileSystem(uploadedFile.getInputstream());
        HSSFWorkbook wb = new HSSFWorkbook(fs);
        HSSFSheet sheet = wb.getSheetAt(0);
        GlossaryEntryGlossaryRowBuilder glossaryEntryRowBuilder = null;

        List<GlossaryEntry> glossaryEntries = new ArrayList<>();

        for(int rowNumber = 0; rowNumber < sheet.getPhysicalNumberOfRows(); ++rowNumber)
        {
            if(isEmptyRow(sheet.getRow(rowNumber)))
            {
                continue;
            }
            if(glossaryEntryRowBuilder == null)
            {
                glossaryEntryRowBuilder = new GlossaryEntryGlossaryRowBuilder();
                errorsDuringProcessing = glossaryEntryRowBuilder.headerInit(sheet.getRow(rowNumber), languageMap);
                if(!errorsDuringProcessing.isEmpty())
                {
                    log.error("Errors during header processing, can`t continue.");
                    return;
                }
            }
            else
            {
                glossaryEntries.add(glossaryEntryRowBuilder.build(sheet.getRow(rowNumber)));
            }
        }

        sortedGlossaryEntries = joinEntries(glossaryEntries);
    }

    private List<GlossaryEntry> joinEntries(final List<GlossaryEntry> glossaryEntries)
    {
        List<GlossaryEntry> result = new ArrayList<>();
        Iterator<GlossaryEntry> glossEntryIterator = glossaryEntries.iterator();
        while(glossEntryIterator.hasNext())
        {
            GlossaryEntry entry = glossEntryIterator.next();
            final AtomicBoolean alreadyExist = new AtomicBoolean(false);
            result.forEach(resultGlossary ->
            {
                if(StringUtils.equals(resultGlossary.getTopicOne(), entry.getTopicOne())
                        && StringUtils.equals(resultGlossary.getTopicTwo(), entry.getTopicTwo())
                        && StringUtils.equals(resultGlossary.getTopicThree(), entry.getTopicThree())
                        && StringUtils.equals(resultGlossary.getDescription(), entry.getDescription()))
                {
                    resultGlossary.getTerms().addAll(entry.getTerms());
                    alreadyExist.set(true);
                }
            });
            if(!alreadyExist.get())
            {
                result.add(entry);
            }
            glossEntryIterator.remove();
        }
        ;
        return result;
    }

    /**
     * @return the parsed glossary entries
     */
    public List<GlossaryEntry> getEntries()
    {
        return sortedGlossaryEntries;
    }

}
