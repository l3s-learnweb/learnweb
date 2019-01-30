package de.l3s.learnweb.resource.glossaryNew;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.font.FontRenderContext;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

import javax.faces.application.FacesMessage;
import javax.faces.event.AjaxBehaviorEvent;
import javax.faces.model.SelectItem;
import javax.faces.view.ViewScoped;
import javax.imageio.ImageIO;
import javax.inject.Named;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFCellStyle;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;
import org.apache.poi.ss.usermodel.ClientAnchor;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.Drawing;
import org.apache.poi.ss.usermodel.Workbook;

import com.lowagie.text.Document;
import com.lowagie.text.PageSize;

import de.l3s.learnweb.beans.ApplicationBean;
import de.l3s.learnweb.logging.Action;
import de.l3s.learnweb.user.Organisation.Option;
import de.l3s.learnweb.user.User;
import de.l3s.util.BeanHelper;

@Named
@ViewScoped
public class GlossaryBeanNEW extends ApplicationBean implements Serializable
{
    private static final long serialVersionUID = 7104637880221636543L;
    private static final Logger log = Logger.getLogger(GlossaryBeanNEW.class);

    private int resourceId;
    private GlossaryResource glossaryResource;
    private List<GlossaryTableView> tableItems;
    private List<GlossaryTableView> filteredTableItems;
    private GlossaryEntry formEntry;
    private final List<SelectItem> availableTopicOne = new ArrayList<>();
    private List<SelectItem> availableTopicTwo = new ArrayList<>();
    private List<SelectItem> availableTopicThree = new ArrayList<>();
    private boolean paginator = true;
    private String toggleLabel = "Show All";
    private boolean optionMandatoryDescription;
    private List<Locale> tableLanguageFilter;

    public void onLoad()
    {
        try
        {
            User user = getUser();
            if(user == null)
                return;
            glossaryResource = getLearnweb().getGlossaryManager().getGlossaryResource(resourceId);
            if(glossaryResource == null)
            {
                log.error("Error in loading glossary resource for resource ID: " + resourceId + "\n" + BeanHelper.getRequestSummary());
                addInvalidParameterMessage("resource_id");
                return;
            }
            log(Action.glossary_open, glossaryResource);

            availableTopicOne.add(new SelectItem("Environment"));
            availableTopicOne.add(new SelectItem("European Politics"));
            availableTopicOne.add(new SelectItem("Medicine"));
            availableTopicOne.add(new SelectItem("Tourism"));

            // convert tree like glossary structure to flat table
            tableItems = getLearnweb().getGlossaryManager().convertToGlossaryTableView(glossaryResource);
            setFilteredTableItems(tableItems);

            setNewFormEntry();

            tableLanguageFilter = new ArrayList<>(glossaryResource.getAllowedLanguages());

            optionMandatoryDescription = user.getOrganisation().getOption(Option.Glossary_Mandatory_Description);
        }
        catch(Exception e)
        {
            addFatalMessage(e);
        }
    }

    public void setGlossaryForm(GlossaryTableView tableItem)
    {
        try
        {
            //set form entry
            formEntry = tableItem.getEntry().clone();
            //Reset ID to old entry ID as it is not copy action
            formEntry.setId(tableItem.getEntryId()); //entry ID
            //Reset original entry ID as it is not a copy action
            formEntry.setOriginalEntryId(tableItem.getEntry().getOriginalEntryId());
            //Reset old term ID and original term id as it is not a copy action
            for(GlossaryTerm term : formEntry.getTerms())
            {
                term.setId(term.getOriginalTermId());
                term.setOriginalTermId(tableItem.getEntry().getTerm(term.getId()).getOriginalTermId());
            }
        }
        catch(Exception e)
        {
            addFatalMessage(e);
        }
    }

    public String onSave()
    {
        try
        {
            //logging
            if(formEntry.getId() > 1)
                log(Action.glossary_entry_edit, glossaryResource.getGroupId(), formEntry.getId(), "", getUser());

            formEntry.setLastChangedByUserId(getUser().getId());

            //to reset fulltext search
            formEntry.setFulltext(null);

            //set last changed by user id for terms
            for(GlossaryTerm term : formEntry.getTerms())
            {
                // TODO check if the term was really modified

                term.setLastChangedByUserId(getUser().getId());
                //log term edit actions
                if(term.getId() > 0)
                    log(Action.glossary_term_edit, glossaryResource, glossaryResource.getGroupId());
            }

            if(formEntry.getTerms().size() == numberOfDeletedTerms())
            {
                addMessage(FacesMessage.SEVERITY_ERROR, getLocaleMessage("Glossary.entry_validation"));
                setKeepMessages();
                return null;
            }

            formEntry.setLastChangedByUserId(getUser().getId());
            getLearnweb().getGlossaryManager().saveEntry(formEntry, glossaryResource);
            addMessage(FacesMessage.SEVERITY_INFO, getLocaleMessage("Changes_saved"));
            setKeepMessages();
        }
        catch(SQLException e)
        {
            log.error("Unable to save entry for resource " + formEntry.getResourceId() + ", entry ID: " + formEntry.getId(), e);
            addFatalMessage(e);
            setKeepMessages();
            return null;

        }
        setNewFormEntry();
        return "/lw/glossary/glossary.jsf?resource_id=" + getResourceId() + "&faces-redirect=true";
    }

    /**
     * Method undoes any action taken on formEntry on click of "Cancel"
     */
    public String onCancel()
    {
        setNewFormEntry();
        return "/lw/glossary/glossary.jsf?resource_id=" + getResourceId() + "&faces-redirect=true";
    }

    public void setNewFormEntry()
    {
        formEntry = new GlossaryEntry();
        formEntry.setResourceId(resourceId);

        // add two terms
        addTerm();
        addTerm();
    }

    public String deleteEntry(GlossaryTableView row)
    {
        try
        {
            log(Action.glossary_entry_delete, glossaryResource.getGroupId(), row.getEntryId(), "", getUser());
            row.getEntry().setDeleted(true);
            row.getEntry().setLastChangedByUserId(getUser().getId());

            getLearnweb().getGlossaryManager().saveEntry(row.getEntry(), glossaryResource);
            //Remove entry from resource
            glossaryResource.getEntries().remove(row.getEntry());
            addMessage(FacesMessage.SEVERITY_INFO, getLocaleMessage("Glossary.deleted") + "!");
            setKeepMessages();
            return "/lw/glossary/glossary.jsf?resource_id=" + getResourceId() + "&faces-redirect=true";
        }
        catch(SQLException e)
        {
            log.error("Unable to delete entry for resource " + row.getEntry().getResourceId() + ", entry ID: " + row.getEntry().getId(), e);
            addFatalMessage(e);
            setKeepMessages();
            return null;
        }

    }

    public void deleteTerm(GlossaryTerm term)
    {
        try
        {
            term.setDeleted(true);
            if(formEntry.getTerms().size() <= 1 || numberOfDeletedTerms() == formEntry.getTerms().size())
            {
                term.setDeleted(false);//reset deletion
                addMessage(FacesMessage.SEVERITY_INFO, getLocaleMessage("Glossary.term_validation"));
                return;
            }
            if(term.getId() <= 0) //Its a new term. Safe to remove here.
            {
                formEntry.getTerms().remove(term);
            }
            formEntry.setFulltext(null);
            addMessage(FacesMessage.SEVERITY_INFO, getLocaleMessage("Glossary.term") + ": " + term.getTerm() + " " + getLocaleMessage("Glossary.deleted") + "!");
            log(Action.glossary_term_delete, glossaryResource.getGroupId(), term.getId(), "", getUser());
        }
        catch(Exception e)
        {
            addFatalMessage(e);
        }
    }

    private int numberOfDeletedTerms()
    {
        int deletedTerms = 0;

        for(GlossaryTerm t : formEntry.getTerms())
        {
            if(t.isDeleted())
                deletedTerms++;
        }
        return deletedTerms;
    }

    public void addTerm()
    {
        try
        {
            GlossaryTerm newTerm = new GlossaryTerm();

            // find a language that is not used yet in this entry
            List<Locale> unusedLanguages = new ArrayList<>(glossaryResource.getAllowedLanguages());
            for(GlossaryTerm term : formEntry.getTerms())
            {
                unusedLanguages.remove(term.getLanguage());
            }
            if(unusedLanguages.size() == 0) // all languages have been used in this glossary
                unusedLanguages = glossaryResource.getAllowedLanguages();

            newTerm.setLanguage(unusedLanguages.get(0));
            formEntry.addTerm(newTerm);
        }
        catch(Exception e)
        {
            addFatalMessage(e);
        }
    }

    public void changeTopicOne(AjaxBehaviorEvent event)
    {
        createAvailableTopicsTwo();
        formEntry.setTopicTwo("");
        formEntry.setTopicThree("");
        availableTopicThree.clear();
    }

    private void createAvailableTopicsTwo()
    {
        availableTopicTwo.clear();

        if(formEntry.getTopicOne().equalsIgnoreCase("medicine"))
        {
            availableTopicTwo.add(new SelectItem("Diseases and disorders"));
            availableTopicTwo.add(new SelectItem("Anatomy"));
            availableTopicTwo.add(new SelectItem("Medical branches"));
            availableTopicTwo.add(new SelectItem("Institutions"));
            availableTopicTwo.add(new SelectItem("Professions"));
            availableTopicTwo.add(new SelectItem("Food and nutrition"));
            availableTopicTwo.add(new SelectItem("other"));
        }
        else if(formEntry.getTopicOne().equalsIgnoreCase("TOURISM"))
        {
            availableTopicTwo.add(new SelectItem("Accommodation"));
            availableTopicTwo.add(new SelectItem("Surroundings"));
            availableTopicTwo.add(new SelectItem("Heritage"));
            availableTopicTwo.add(new SelectItem("Food and Produce"));
            availableTopicTwo.add(new SelectItem("Activities and Tours"));
            availableTopicTwo.add(new SelectItem("Travel and Transport"));
        }
    }

    public void changeTopicTwo(AjaxBehaviorEvent event)
    {
        createAvailableTopicsThree();
        formEntry.setTopicThree("");
    }

    private void createAvailableTopicsThree()
    {
        availableTopicThree.clear();

        if(formEntry.getTopicOne().equalsIgnoreCase("medicine"))
        {
            if(formEntry.getTopicTwo().equalsIgnoreCase("Diseases and disorders"))
            {
                availableTopicThree.add(new SelectItem("Signs and symptoms"));
                availableTopicThree.add(new SelectItem("Diagnostic techniques"));
                availableTopicThree.add(new SelectItem("Therapies"));
                availableTopicThree.add(new SelectItem("Drugs"));
            }
            else if(formEntry.getTopicTwo().equalsIgnoreCase("Anatomy"))
            {
                availableTopicThree.add(new SelectItem("Organs"));
                availableTopicThree.add(new SelectItem("Bones"));
                availableTopicThree.add(new SelectItem("Muscles"));
                availableTopicThree.add(new SelectItem("Other"));
            }
        }
        if(formEntry.getTopicOne().equalsIgnoreCase("TOURISM"))
        {
            if(formEntry.getTopicTwo().equalsIgnoreCase("Heritage"))
            {
                availableTopicThree.add(new SelectItem("History"));
                availableTopicThree.add(new SelectItem("Architecture"));
                availableTopicThree.add(new SelectItem("Festivals"));
            }
        }
    }

    public void togglePaginator()
    {
        paginator = !paginator;
        toggleLabel = toggleLabel.equalsIgnoreCase("show all") ? getLocaleMessage("Glossary.collapse") : getLocaleMessage("Glossary.show_all");
    }

    public String getToggleLabel()
    {
        return toggleLabel;
    }

    public void parseXls(File file)
    {
        User user = getUser();
        if(user == null)
            return;

        try
        {
            POIFSFileSystem fs = new POIFSFileSystem(new FileInputStream(file));
            HSSFWorkbook wb = new HSSFWorkbook(fs);
            HSSFSheet sheet = wb.getSheetAt(0);
            HSSFRow row;
            HSSFCell cell;

            int amountOfRows;
            amountOfRows = sheet.getPhysicalNumberOfRows() - 1;

            // int amountOfColumns = 11;

            setNewFormEntry();

            for(int r = 1; r < amountOfRows; r++)
            {
                row = sheet.getRow(r);
                if(row != null)
                {
                    formEntry.setTopicOne(row.getCell(0).getStringCellValue());
                    formEntry.setTopicTwo(row.getCell(1).getStringCellValue());
                    formEntry.setTopicThree(row.getCell(2).getStringCellValue());
                    formEntry.setDescription(row.getCell(3).getStringCellValue());

                    List<GlossaryTerm> terms = new LinkedList<>();
                    for(int i = 1; i < amountOfRows; i++)
                    {
                        String topic1 = row.getCell(0).getStringCellValue();
                        String topic2 = row.getCell(1).getStringCellValue();
                        String topic3 = row.getCell(2).getStringCellValue();
                        String description = row.getCell(3).getStringCellValue();
                        if(topic1 == formEntry.getTopicOne() & topic2 == formEntry.getTopicTwo() &
                                topic3 == formEntry.getTopicThree() & description == formEntry.getDescription())
                        {
                            GlossaryTerm newTerm = new GlossaryTerm();
                            newTerm.setTerm(row.getCell(4).getStringCellValue());
                            newTerm.setLanguage(newTerm.stringToLocaleLanguage(row.getCell(4).getStringCellValue()));
                            String usesString = row.getCell(6).getStringCellValue();
                            List<String> uses = Arrays.asList(usesString.split("\\s*,\\s*"));;
                            newTerm.setUses(uses);
                            newTerm.setPronounciation(row.getCell(7).getStringCellValue());
                            newTerm.setAcronym(row.getCell(8).getStringCellValue());
                            newTerm.setSource(row.getCell(9).getStringCellValue());
                            newTerm.setPhraseology(row.getCell(10).getStringCellValue());

                            terms.add(newTerm);
                        }
                    }
                    formEntry.setTerms(terms);
                }
                getLearnweb().getGlossaryManager().saveEntry(formEntry, glossaryResource);
                addMessage(FacesMessage.SEVERITY_INFO, getLocaleMessage("Changes_saved"));
                setKeepMessages();
                setNewFormEntry();
            }
        }
        catch(Exception e)
        {
            log.error("Error during parsing glossary xls", e);
            addFatalMessage(e);
        }
    }

    public void postProcessXls(Object document)
    {
        User user = getUser();
        if(user == null)
            return;

        try
        {
            if(user.getOrganisation().getOption(Option.Glossary_Add_Watermark))
            {
                log.debug("post processing glossary xls");

                HSSFWorkbook wb = (HSSFWorkbook) document;

                HSSFSheet sheet = wb.getSheetAt(0);

                HSSFRow row0 = sheet.getRow(1);
                HSSFCell cell0;

                File watermark = text2Image(getLearnweb().getResourceManager().getResource(resourceId).getUser().getUsername());

                InputStream is = new FileInputStream(watermark);

                byte[] bytes = IOUtils.toByteArray(is);
                int pictureIdx = wb.addPicture(bytes, Workbook.PICTURE_TYPE_PNG);
                is.close();
                CreationHelper helper = wb.getCreationHelper();
                Drawing drawing = sheet.createDrawingPatriarch();
                ClientAnchor anchor = helper.createClientAnchor();
                anchor.setAnchorType(ClientAnchor.AnchorType.DONT_MOVE_AND_RESIZE);

                if(row0 != null)
                {
                    cell0 = row0.getCell(0);
                }
                else
                {

                    return;
                }

                for(int i = 2; i <= sheet.getLastRowNum(); i++)
                {
                    HSSFRow row = sheet.getRow(i);
                    HSSFCell cell = row.getCell(0);

                    if(cell != null)
                    {
                        cell.setCellValue(cell.getStringCellValue());
                        if(cell.getStringCellValue().equals(cell0.getStringCellValue()))
                        {
                            cell0.setCellValue(cell0.getStringCellValue());
                            continue;
                        }
                        else
                        {
                            int rowIndex = i;
                            if(sheet.getRow(rowIndex).getCell(0) != null)
                            {
                                cell0 = sheet.getRow(rowIndex).getCell(0);
                                sheet.shiftRows(rowIndex, sheet.getLastRowNum(), 1);
                            }
                        }
                    }

                }

                //Set owner details

                //set top-left corner of the picture,
                //subsequent call of Picture#resize() will operate relative to it
                int row1 = (sheet.getLastRowNum() / 4);
                int row2 = ((sheet.getLastRowNum() / 4) * 3);
                anchor.setCol1(3);
                anchor.setRow1(row1);
                anchor.setCol2(10);
                anchor.setRow2(row2);

                //Picture pict =
                drawing.createPicture(anchor, pictureIdx);
                HSSFCellStyle copyrightStyle = wb.createCellStyle();
                copyrightStyle.setLocked(true);
                sheet.protectSheet("learnweb");
                watermark.delete();
            }
        }
        catch(Exception e)
        {
            log.error("Error in postprocessing Glossary xls for resource: " + resourceId, e);
            addFatalMessage(e);
        }

    }

    public File text2Image(String textString) throws IOException
    {
        //create a File Object

        File file = File.createTempFile(textString, ".png");
        //File file = new File("./uploaded_files/tmp/" + textString + ".png");
        //create the font you wish to use
        Font font = new Font("Tahoma", Font.PLAIN, 18);

        //create the FontRenderContext object which helps us to measure the text
        FontRenderContext frc = new FontRenderContext(null, true, true);

        //get the height and width of the text
        Rectangle2D bounds = font.getStringBounds(textString, frc);
        int width = (int) bounds.getWidth();
        int height = (int) bounds.getHeight();

        //create a BufferedImage object
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

        //calling createGraphics() to get the Graphics2D
        Graphics2D graphic = image.createGraphics();

        //set color and other parameters
        /*Color background = new Color(1f, 1f, 1f, 0.0f);
        
        graphic.setColor(background);
        graphic.setBackground(background);*/
        graphic.setComposite(AlphaComposite.getInstance(AlphaComposite.CLEAR));
        graphic.fillRect(0, 0, width, height);
        graphic.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.5f));
        Color textColor = new Color(0, 0, 0, 0.5f);
        graphic.setColor(textColor);
        graphic.setFont(font);
        graphic.drawString(textString, (float) bounds.getX(), (float) -bounds.getY());

        //releasing resources
        graphic.dispose();

        //creating the file
        ImageIO.write(image, "png", file);

        return file;
    }

    public void rotatePDF(Object document)
    {
        Document doc = (Document) document;
        doc.setPageSize(PageSize.A4.rotate());
    }

    public int getResourceId()
    {
        return resourceId;
    }

    public void setResourceId(int resourceId)
    {
        this.resourceId = resourceId;
    }

    public GlossaryResource getGlossaryResource()
    {
        return glossaryResource;
    }

    public List<GlossaryTableView> getTableItems()
    {
        return tableItems;
    }

    public List<GlossaryTableView> getFilteredTableItems()
    {
        return filteredTableItems;
    }

    public void setFilteredTableItems(List<GlossaryTableView> filteredTableItems)
    {
        this.filteredTableItems = filteredTableItems;
    }

    public int getEntryCount()
    {
        return glossaryResource.getEntries().size();

    }

    public GlossaryEntry getFormEntry()
    {
        return formEntry;
    }

    public List<SelectItem> getAvailableTopicOne()
    {
        return availableTopicOne;
    }

    public List<SelectItem> getAvailableTopicTwo()
    {
        return availableTopicTwo;
    }

    public List<SelectItem> getAvailableTopicThree()
    {
        return availableTopicThree;
    }

    private transient List<SelectItem> allowedTermLanguages; // cache for the allowed languages select list

    public List<SelectItem> getAllowedTermLanguages()
    {
        if(null == allowedTermLanguages && glossaryResource != null)
        {
            allowedTermLanguages = localesToSelectitems(glossaryResource.getAllowedLanguages());
        }
        return allowedTermLanguages;
    }

    public boolean isPaginator()
    {
        return paginator;
    }

    public boolean isOptionMandatoryDescription()
    {
        return optionMandatoryDescription;
    }

    /*
    public List<ColumnModel> getColumns()
    {
        List<ColumnModel> columns = new ArrayList<>();

        columns.add(new ColumnModel("uses", "uses"));
        columns.add(new ColumnModel("Pronunciation", "pronounciation"));
        columns.add(new ColumnModel("uses", "source"));
        columns.add(new ColumnModel("uses", "phraseology"));

        return columns;
    }


    private void createDynamicColumns() {
        String[] columnKeys = columnTemplate.split(" ");
        columns = new ArrayList<ColumnModel>();

        for(String columnKey : columnKeys) {
            String key = columnKey.trim();

            if(VALID_COLUMN_KEYS.contains(key)) {
                columns.add(new ColumnModel(columnKey.toUpperCase(), columnKey));
            }
        }
    }

    public void updateColumns()
    {
        //reset table state
        UIComponent table = FacesContext.getCurrentInstance().getViewRoot().findComponent(":form:cars");
        table.setValueExpression("sortBy", null);

        //update columns
        createDynamicColumns();
    }

    static public class ColumnModel implements Serializable
    {

        private String header;
        private String property;

        public ColumnModel(String header, String property)
        {
            this.header = header;
            this.property = property;
        }

        public String getHeader()
        {
            return header;
        }

        public String getProperty()
        {
            return property;
        }
    }*/

    public List<Locale> getTableLanguageFilter()
    {
        return tableLanguageFilter;
    }

    public void setTableLanguageFilter(List<Locale> tableLanguageFilter)
    {
        this.tableLanguageFilter = tableLanguageFilter;
    }

}
