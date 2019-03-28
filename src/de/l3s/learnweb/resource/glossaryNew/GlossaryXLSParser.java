package de.l3s.learnweb.resource.glossaryNew;

import de.l3s.learnweb.resource.glossaryNew.builders.GlossaryEntryGlossaryRowBuilder;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;
import org.apache.poi.ss.usermodel.Row;
import org.primefaces.model.UploadedFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class GlossaryXLSParser
{
    private List<GlossaryEntry> sortedGlossaryEntries;
    private UploadedFile uploadedFile;
    private Map<String, Locale> languageMap;

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
                glossaryEntryRowBuilder.headerInit(sheet.getRow(rowNumber), languageMap);
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
        //TODO IMPLEMENT ME, after xhtml fixing
        return glossaryEntries;
    }

    /**
     * @return the parsed glossary entries
     */
    public List<GlossaryEntry> getEntries()
    {
        return sortedGlossaryEntries;
    }

}
