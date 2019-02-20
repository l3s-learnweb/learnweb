package de.l3s.learnweb.resource.glossaryNew;

import java.util.*;

import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;
import org.primefaces.model.UploadedFile;

import javax.faces.application.FacesMessage;

public class GlossaryXLSParser
{

    private List<GlossaryEntry> glossaryEntries;
    private UploadedFile uploadedFile;
    private HashMap<String, Locale> languageMap;

    public GlossaryXLSParser(UploadedFile uploadedFile, HashMap<String, Locale> languageMap)
    {
        this.uploadedFile = uploadedFile;
        this.languageMap = languageMap;
    }

    /**
     *
     * @return true if the file can be parsed without errors
     */
    public boolean isValid()
    {
        // TODO Auto-generated method stub
        //check amount of columns?
        //check languages?
        return false;
    }

    public void parseGlossaryEntries(){
        try
        {
            POIFSFileSystem fs = new POIFSFileSystem(uploadedFile.getInputstream());
            HSSFWorkbook wb = new HSSFWorkbook(fs);
            HSSFSheet sheet = wb.getSheetAt(0);
            HSSFRow row;
            //HSSFCell cell;

            int amountOfRows;
            amountOfRows = sheet.getPhysicalNumberOfRows() - 1;//-1 because of hedaer

            // int amountOfColumns = 11;

            GlossaryEntry glossaryEntry = new GlossaryEntry();

            for(int r = 1; r < amountOfRows; r++)
            {
                row = sheet.getRow(r);
                if(row != null)
                {
                    glossaryEntry.setTopicOne(row.getCell(0).getStringCellValue());
                    glossaryEntry.setTopicTwo(row.getCell(1).getStringCellValue());
                    glossaryEntry.setTopicThree(row.getCell(2).getStringCellValue());
                    glossaryEntry.setDescription(row.getCell(3).getStringCellValue());

                    List<GlossaryTerm> terms = new LinkedList<>();
                    for(int i = 1; i < amountOfRows; i++)
                    {
                        String topic1 = row.getCell(0).getStringCellValue();
                        String topic2 = row.getCell(1).getStringCellValue();
                        String topic3 = row.getCell(2).getStringCellValue();
                        String description = row.getCell(3).getStringCellValue();
                        if(topic1 == glossaryEntry.getTopicOne() & topic2 == glossaryEntry.getTopicTwo() &
                                topic3 == glossaryEntry.getTopicThree() & description == glossaryEntry.getDescription())
                        {
                            GlossaryTerm newTerm = new GlossaryTerm();
                            newTerm.setTerm(row.getCell(4).getStringCellValue());
                            //TODO: change language setting below
                            // newTerm.setLanguage(newTerm.stringToLocaleLanguage(row.getCell(4).getStringCellValue()));
                            String usesString = row.getCell(6).getStringCellValue();
                            List<String> uses = Arrays.asList(usesString.split("\\s*,\\s*"));
                            ;
                            newTerm.setUses(uses);
                            newTerm.setPronounciation(row.getCell(7).getStringCellValue());
                            newTerm.setAcronym(row.getCell(8).getStringCellValue());
                            newTerm.setSource(row.getCell(9).getStringCellValue());
                            newTerm.setPhraseology(row.getCell(10).getStringCellValue());

                            terms.add(newTerm);
                        }
                    }
                    glossaryEntry.setTerms(terms);
                }
                // don't save anything until we are sure that the parser works as expected getLearnweb().getGlossaryManager().saveEntry(formEntry, glossaryResource);

                // addMessage(FacesMessage.SEVERITY_INFO, getLocaleMessage("Changes_saved"));
                // setKeepMessages();
                // setNewFormEntry();
            }
        }
        catch(Exception e)
        {
            // log.error("Error during parsing glossary xls", e);
            // addErrorMessage(e);
        }
    }

    /**
     *
     * @return the parsed glossary entries
     */
    public List<GlossaryEntry> getEntries()
    {
        return glossaryEntries;
    }

    public List<String> getErrorMessages()
    {
        // TODO Auto-generated method stub
        return null;
    }

}
