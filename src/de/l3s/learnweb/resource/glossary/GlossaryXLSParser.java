package de.l3s.learnweb.resource.glossary;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.log4j.Logger;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;
import org.apache.poi.ss.usermodel.Row;
import org.primefaces.model.UploadedFile;

import java.io.IOException;
import java.util.*;

public class GlossaryXLSParser
{
    private static final Logger log = Logger.getLogger(GlossaryXLSParser.class);

    private List<GlossaryEntry> sortedGlossaryEntries = new ArrayList<>();
    private UploadedFile uploadedFile;
    private Map<String, Locale> languageMap;

    public GlossaryImportResponse getImportResponse()
    {
        return importResponse;
    }

    private GlossaryImportResponse importResponse = new GlossaryImportResponse();

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
        GlossaryRowBuilder glossaryRowBuilder = null;

        List<GlossaryEntry> glossaryEntries = new ArrayList<>();

        for(int rowNumber = 0; rowNumber < sheet.getPhysicalNumberOfRows(); ++rowNumber)
        {
            if(isEmptyRow(sheet.getRow(rowNumber)))
            {
                continue;
            }
            if(glossaryRowBuilder == null)
            {
                glossaryRowBuilder = new GlossaryRowBuilder();
                importResponse.addErrors(glossaryRowBuilder.headerInit(sheet.getRow(rowNumber), languageMap));
                if(!importResponse.isSuccessful())
                {
                    log.error("Errors during header processing, can`t continue.");
                    return;
                }
            }
            else
            {
                Pair<GlossaryEntry, List<ParsingError>> entriesWithErrors = glossaryRowBuilder.build(sheet.getRow(rowNumber));
                if(entriesWithErrors.getValue().isEmpty()){
                    glossaryEntries.add(entriesWithErrors.getKey());
                } else{
                    importResponse.addErrors(entriesWithErrors.getValue());
                }
            }
        }
        sortedGlossaryEntries = joinEntries(glossaryEntries);
        importResponse.setAmountOfEntries(sortedGlossaryEntries.size());
    }

    private List<GlossaryEntry> joinEntries(final List<GlossaryEntry> glossaryEntries)
    {
        List<GlossaryEntry> result = new ArrayList<>();
        for(final GlossaryEntry entry : glossaryEntries)
        {
            boolean alreadyExist = false;
            if(!result.isEmpty() && StringUtils.equals(result.get(result.size() - 1).getTopicOne(), entry.getTopicOne())
                    && StringUtils.equals(result.get(result.size() - 1).getTopicTwo(), entry.getTopicTwo())
                    && StringUtils.equals(result.get(result.size() - 1).getTopicThree(), entry.getTopicThree())
                    && StringUtils.equals(result.get(result.size() - 1).getDescription(), entry.getDescription()))
            {
                result.get(result.size() - 1).getTerms().addAll(entry.getTerms());
                alreadyExist = true;
            }
            if(!alreadyExist)
            {
                result.add(entry);
            }
        }
        return result;
    }

    public List<GlossaryEntry> getEntries()
    {
        return sortedGlossaryEntries;
    }

}
