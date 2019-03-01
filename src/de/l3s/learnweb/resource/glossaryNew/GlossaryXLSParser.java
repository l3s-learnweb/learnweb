package de.l3s.learnweb.resource.glossaryNew;

import java.util.*;

import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;
import org.primefaces.model.UploadedFile;

import javax.faces.application.FacesMessage;

public class GlossaryXLSParser extends GlossaryBeanNEW
{

    private List<GlossaryEntry> glossaryEntries;
    private UploadedFile uploadedFile;
    private HashMap<String, Locale> languageMap;
    private List<String> errorMessages;

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
        boolean isValid = false;
        try
        {
            POIFSFileSystem fs = new POIFSFileSystem(uploadedFile.getInputstream());
            HSSFWorkbook wb = new HSSFWorkbook(fs);
            HSSFSheet sheet = wb.getSheetAt(0);
            HSSFRow row;
            if(sheet.getRow(0).getPhysicalNumberOfCells() == 11)
            {
                for(int r = 1; r < sheet.getPhysicalNumberOfRows() - 1; r++)
                {
                    row = sheet.getRow(r);
                    if(row != null)
                    {
                        if(languageMap.containsKey(row.getCell(0).getStringCellValue()))
                        {
                            isValid = true;
                        }
                        else
                        {
                            errorMessages.add("There are incorrect languages in the file");
                            isValid = false;
                            break;
                        }
                    }
                }
            }
            else
            {
                errorMessages.add("Amount of columns does not correspond to the table");
                isValid = false;
            }
        } catch(Exception e){
            addMessage(FacesMessage.SEVERITY_ERROR, getLocaleMessage("Error during parsing glossary xls"));
            setKeepMessages();
            //log.error("Error during parsing glossary xls", e);
            //addErrorMessage(e);
        }

        return isValid;
    }

    public void parseGlossaryEntries(){
        try
        {
            POIFSFileSystem fs = new POIFSFileSystem(uploadedFile.getInputstream());
            HSSFWorkbook wb = new HSSFWorkbook(fs);
            HSSFSheet sheet = wb.getSheetAt(0);
            HSSFRow row;

            int amountOfRows;
            amountOfRows = sheet.getPhysicalNumberOfRows() - 1;//-1 because of header

            GlossaryEntry currentGlossaryEntry = new GlossaryEntry();
            List<GlossaryTerm> terms = new LinkedList<>();

            for(int r = 1; r < amountOfRows; r++)
            {
                row = sheet.getRow(r);
                if(row != null)
                {
                    String topic1 = row.getCell(0).getStringCellValue();
                    String topic2 = row.getCell(1).getStringCellValue();
                    String topic3 = row.getCell(2).getStringCellValue();
                    String description = row.getCell(3).getStringCellValue();

                    if(topic1.equals(currentGlossaryEntry.getTopicOne()) && topic2.equals(currentGlossaryEntry.getTopicTwo()) &&
                            topic3.equals(currentGlossaryEntry.getTopicThree()) && description.equals(currentGlossaryEntry.getDescription()))
                    {
                        GlossaryTerm newTerm = new GlossaryTerm();
                        newTerm.setTerm(row.getCell(4).getStringCellValue());
                        newTerm.setLanguage(languageMap.get(row.getCell(4).getStringCellValue()));
                        String usesString = row.getCell(6).getStringCellValue();
                        List<String> uses = Arrays.asList(usesString.split("\\s*,\\s*"));
                        newTerm.setUses(uses);
                        newTerm.setPronounciation(row.getCell(7).getStringCellValue());
                        newTerm.setAcronym(row.getCell(8).getStringCellValue());
                        newTerm.setSource(row.getCell(9).getStringCellValue());
                        newTerm.setPhraseology(row.getCell(10).getStringCellValue());

                        terms.add(newTerm);
                        currentGlossaryEntry.setTerms(terms);
                    }
                    else
                    {
                        glossaryEntries.add(currentGlossaryEntry);

                        currentGlossaryEntry = new GlossaryEntry();
                        currentGlossaryEntry.setTopicOne(topic1);
                        currentGlossaryEntry.setTopicTwo(topic2);
                        currentGlossaryEntry.setTopicThree(topic3);
                        currentGlossaryEntry.setDescription(description);

                        GlossaryTerm newTerm = new GlossaryTerm();
                        newTerm.setTerm(row.getCell(4).getStringCellValue());
                        newTerm.setLanguage(languageMap.get(row.getCell(4).getStringCellValue()));
                        String usesString = row.getCell(6).getStringCellValue();
                        List<String> uses = Arrays.asList(usesString.split("\\s*,\\s*"));
                        newTerm.setUses(uses);
                        newTerm.setPronounciation(row.getCell(7).getStringCellValue());
                        newTerm.setAcronym(row.getCell(8).getStringCellValue());
                        newTerm.setSource(row.getCell(9).getStringCellValue());
                        newTerm.setPhraseology(row.getCell(10).getStringCellValue());

                        terms.clear();
                        terms.add(newTerm);
                        currentGlossaryEntry.setTerms(terms);
                    }
                    // don't save anything until we are sure that the parser works as expected getLearnweb().getGlossaryManager().saveEntry(formEntry, glossaryResource);
                }
            }
            glossaryEntries.add(currentGlossaryEntry);
            addMessage(FacesMessage.SEVERITY_INFO, getLocaleMessage("Changes_saved"));
            setKeepMessages();
        }
        catch(Exception e)
        {
            addMessage(FacesMessage.SEVERITY_ERROR, getLocaleMessage("Error during parsing glossary xls"));
            setKeepMessages();
            //log.error("Error during parsing glossary xls", e);
            //addErrorMessage(e);
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
        return errorMessages;
    }

}
