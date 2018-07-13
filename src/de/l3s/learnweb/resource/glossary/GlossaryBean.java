package de.l3s.learnweb.resource.glossary;

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
import java.net.MalformedURLException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.context.FacesContext;
import javax.faces.event.AjaxBehaviorEvent;
import javax.faces.model.SelectItem;
import javax.imageio.ImageIO;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFCellStyle;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.ClientAnchor;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.Drawing;
import org.apache.poi.ss.usermodel.Picture;
import org.apache.poi.ss.usermodel.Workbook;

import com.lowagie.text.BadElementException;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Image;
import com.lowagie.text.PageSize;

import de.l3s.learnweb.LogEntry.Action;
import de.l3s.learnweb.beans.ApplicationBean;
import de.l3s.learnweb.resource.glossary.LanguageItem.LANGUAGE;
import de.l3s.learnweb.user.User;

@ViewScoped
@ManagedBean
public class GlossaryBean extends ApplicationBean implements Serializable
{
    private static final long serialVersionUID = -1811030091337893637L;
    private static final Logger log = Logger.getLogger(GlossaryBean.class);
    private static final String PREFERENCE_TOPIC1 = "GLOSSARY_TOPIC1";

    private boolean deleted = false;
    private List<LanguageItem> secondaryLangItems;
    private List<LanguageItem> primaryLangItems;
    private List<LanguageItem> languageItems;
    private final List<String> uses = new ArrayList<String>();
    private String fileName;
    private String selectedTopicOne;
    private String selectedTopicTwo;
    private String selectedTopicThree;
    private String description;
    private final List<SelectItem> availableTopicOnes = new ArrayList<SelectItem>();
    private List<SelectItem> availableTopicTwos = new ArrayList<SelectItem>();
    private List<SelectItem> availableTopicThrees = new ArrayList<SelectItem>();
    private String valueHeaderIt; // TODO does It refer to Italian? then change it
    private int count;
    private int userId;
    private LANGUAGE primaryLanguage;
    private LANGUAGE secondaryLanguage;
    private int resourceId;
    private int groupId; // group id of the resource used only for the logger
    private int glossaryId;
    private List<GlossaryItems> items = new ArrayList<GlossaryItems>();
    private List<GlossaryItems> filteredItems = new ArrayList<GlossaryItems>();
    private GlossaryItems selectedGlossaryItem;
    private boolean paginatorActive = true;

    private int glossaryEntryCount;

    public void onload() throws SQLException
    {
        if(resourceId > 0 && !getLearnweb().getGlossariesManager().checkIfExists(resourceId))
        {
            getGlossaryItems(resourceId);
            setlanguagePair(resourceId);
            setFilteredItems(getItems());
            loadGlossary();
            glossaryEntryCount = getGlossaryEntryCount(resourceId);
            try
            {
                groupId = getLearnweb().getResourceManager().getResource(resourceId).getGroupId();
                log(Action.glossary_open, groupId, resourceId);
            }
            catch(Exception e)
            {
                log.error("Couldn't log glossary action; resource: " + resourceId);
            }

        }
    }

    public void activatePaginator()
    {
        setPaginatorActive(true);
    }

    public void deactivatePaginator()
    {
        setPaginatorActive(false);
    }

    public void rotatePDF(Object document)
    {
        Document doc = (Document) document;
        doc.setPageSize(PageSize.A4.rotate());
    }

    public void loadGlossary()
    {
        //Add topic One
        availableTopicOnes.add(new SelectItem("Environment"));
        availableTopicOnes.add(new SelectItem("European Politics"));
        availableTopicOnes.add(new SelectItem("Medicine"));
        availableTopicOnes.add(new SelectItem("Tourism"));

        uses.add("technical");
        uses.add("popular");
        uses.add("informal");

        if(resourceId > 0)
        {
            if(!getLearnweb().getGlossariesManager().checkIfExists(resourceId))
            {
                createEntry();
                glossaryEntryCount = getGlossaryEntryCount(resourceId);
            }
            else
            {
                resourceId = 0;
                deleted = true;
            }
        }
    }

    private void setlanguagePair(int resourceId2)
    {
        try
        {
            String[] langPair = getLearnweb().getGlossariesManager().getLanguagePairs(resourceId2);
            LanguageItem l = new LanguageItem();
            setPrimaryLanguage(l.getEnum(langPair[0]));
            l = new LanguageItem();
            setSecondaryLanguage(l.getEnum(langPair[1]));

        }
        catch(SQLException e)
        {
            log.error("Error in fetching language pairs for glossary: " + resourceId2, e);
        }

    }

    private int getGlossaryEntryCount(int resourceId)
    {
        int glossEntryCount = getLearnweb().getGlossariesManager().getEntryCount(resourceId);
        return glossEntryCount;

    }

    public void createEntry()
    {
        setDescription("");

        selectedTopicThree = "";
        selectedTopicTwo = "";
        selectedTopicOne = getPreference(PREFERENCE_TOPIC1, availableTopicOnes.get(0).getValue().toString());
        availableTopicThrees.clear();
        createAvailableTopicsTwo();

        secondaryLangItems = new ArrayList<LanguageItem>();
        secondaryLangItems.add(new LanguageItem());
        primaryLangItems = new ArrayList<LanguageItem>();
        primaryLangItems.add(new LanguageItem());
    }

    public void setForm(GlossaryItems gloss)
    {
        createEntry();

        setSelectedTopicOne(gloss.getTopic1());
        createAvailableTopicsTwo();

        setSelectedTopicTwo(gloss.getTopic2());
        createAvailableTopicsThree();

        setSelectedTopicThree(gloss.getTopic3());

        setDescription(gloss.getDescription());
        List<LanguageItem> primaryItemsToSet = new ArrayList<LanguageItem>();
        List<LanguageItem> secondaryItemsToSet = new ArrayList<LanguageItem>();

        //TODO:: Re-factor this part once languages are collapsed into one.
        for(LanguageItem languageItem : gloss.getFinalItems())
        {
            if(languageItem.getLanguage().equals(primaryLanguage))
            {
                languageItem.setUseLabel("Use");
                languageItem.updateUseLabel();
                primaryItemsToSet.add(languageItem);
            }
            else if(languageItem.getLanguage().equals(secondaryLanguage))
            {
                languageItem.setUseLabel("Use");
                languageItem.updateUseLabel();
                secondaryItemsToSet.add(languageItem);
            }
        }

        setSecondaryLangItems(secondaryItemsToSet);
        setPrimaryLangItems(primaryItemsToSet);
        setGlossaryId(gloss.getGlossId());
    }

    public String upload()
    {
        boolean upload = false;

        // TODO what are these loops doing? Do they only check if there exist at least one entry of each language? If yes than the implementation is to complicated
        for(LanguageItem one : getPrimaryLangItems())
        {
            if(!one.getValue().isEmpty() && !upload)
            {
                for(LanguageItem two : getSecondaryLangItems())
                {
                    if(!two.getValue().isEmpty())
                        upload = true;
                    break;
                }
            }
        }
        if(upload)
        {
            GlossaryEntry entry = new GlossaryEntry();

            entry.setDescription(getDescription());
            //entry.setMultimediaFile(getMultimediaFile());
            // gl.setFileName(getFileName());
            entry.setTopicOne(getSelectedTopicOne());
            entry.setTopicTwo(getSelectedTopicTwo());
            entry.setTopicThree(getSelectedTopicThree());
            entry.setFirstLanguageItems(getPrimaryLangItems());
            entry.setUser(getUser());
            entry.setSecondLanguageItems(getSecondaryLangItems());
            entry.setResourceId(getResourceId());
            entry.setGlossaryId(getGlossaryId());

            boolean result = getLearnweb().getGlossariesManager().addToDatabase(entry);
            if(result)
            {
                addMessage(FacesMessage.SEVERITY_INFO, "changes_saved");
                /*
                FacesContext context = FacesContext.getCurrentInstance();
                context.addMessage(null, new FacesMessage("Successful entry"));
                context.getExternalContext().getFlash().setKeepMessages(true);
                */
            }

            if(getGlossaryId() == 0)
                log(Action.glossary_entry_add, groupId, resourceId, Integer.toString(getGlossaryId()));
            else
                log(Action.glossary_entry_edit, groupId, resourceId, Integer.toString(getGlossaryId()));

            createEntry();
            glossaryEntryCount = getGlossaryEntryCount(resourceId);
            //RequestContext.getCurrentInstance().update("main_component");
            return "/lw/showGlossary.jsf?resource_id=" + Integer.toString(getResourceId()) + "&faces-redirect=true";

        }
        else
        {
            addMessage(FacesMessage.SEVERITY_ERROR, "Please enter atleast one valid entry for both language terms"); // TODO use translate
            setKeepMessages(); // is it really necessary?
            return "/lw/showGlossary.jsf?resource_id=" + Integer.toString(getResourceId()) + "&faces-redirect=false";
        }

    }

    public String delete(GlossaryItems item)
    {
        try
        {
            getLearnweb().getGlossariesManager().deleteFromDb(item.getGlossId());
            createEntry();
            log(Action.glossary_entry_delete, groupId, resourceId, item.getGlossId());
        }
        catch(Exception e)
        {
            addFatalMessage(e);
        }
        return "/lw/showGlossary.jsf?resource_id=" + Integer.toString(getResourceId()) + "&faces-redirect=true";
    }

    public void addSecondLanguageItem()
    {
        secondaryLangItems.add(new LanguageItem());
        count++;
        valueHeaderIt = "Term Two" + Integer.toString(count);

        log(Action.glossary_term_add, groupId, resourceId);
    }

    public void removeSecondLanguageItem(LanguageItem item)
    {
        try
        {
            List<LanguageItem> iItems = new ArrayList<LanguageItem>(secondaryLangItems);
            boolean remove = false;

            if(iItems.size() > 1)
                remove = true;

            if(remove)
            {
                secondaryLangItems.remove(item);

                log(Action.glossary_term_delete, groupId, resourceId, item.getTermId()); // TODO log is stored before the change is persisted in the DB, even if a user decides to cancel
            }
            else
            {
                FacesContext context = FacesContext.getCurrentInstance();

                context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error!", "You need atleast one entry of Second Language Terms"));

            }
        }
        catch(Exception e)
        {
            addFatalMessage(e);
        }
    }

    public void removeFirstLanguageItem(LanguageItem item) // TODO duplicated code see removeSecondLanguageItem
    {
        try
        {
            List<LanguageItem> primaryItems = new ArrayList<LanguageItem>(primaryLangItems);
            boolean remove = false;

            if(primaryItems.size() > 1)
                remove = true;

            if(remove)
            {
                primaryLangItems.remove(item);

                log(Action.glossary_term_delete, groupId, resourceId, item.getTermId());
            }
            else
            {
                FacesContext context1 = FacesContext.getCurrentInstance();

                context1.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error!", "You need atleast one entry of First Language Terms"));
            }
        }
        catch(Exception e)
        {
            addFatalMessage(e);
        }
    }

    public void addFirstLanguageItem()
    {
        try
        {
            primaryLangItems.add(new LanguageItem());

            log(Action.glossary_term_add, groupId, resourceId); // should store resourceId + term_id
        }
        catch(Exception e)
        {
            addFatalMessage(e);
        }
    }

    public void changeTopicOne(AjaxBehaviorEvent event)
    {
        createAvailableTopicsTwo();
        selectedTopicTwo = selectedTopicThree = "";
        availableTopicThrees.clear();
    }

    private void createAvailableTopicsTwo()
    {
        availableTopicTwos.clear();

        if(selectedTopicOne.equalsIgnoreCase("medicine"))
        {
            availableTopicTwos.add(new SelectItem("Diseases and disorders"));
            availableTopicTwos.add(new SelectItem("Anatomy"));
            availableTopicTwos.add(new SelectItem("Medical branches"));
            availableTopicTwos.add(new SelectItem("Institutions"));
            availableTopicTwos.add(new SelectItem("Professions"));
            availableTopicTwos.add(new SelectItem("Food and nutrition"));
            availableTopicTwos.add(new SelectItem("other"));
        }
        else if(selectedTopicOne.equalsIgnoreCase("TOURISM"))
        {
            availableTopicTwos.add(new SelectItem("Accommodation"));
            availableTopicTwos.add(new SelectItem("Surroundings"));
            availableTopicTwos.add(new SelectItem("Heritage"));
            availableTopicTwos.add(new SelectItem("Food and Produce"));
            availableTopicTwos.add(new SelectItem("Activities and Tours"));
            availableTopicTwos.add(new SelectItem("Travel and Transport"));
        }
    }

    private void createAvailableTopicsThree()
    {
        availableTopicThrees.clear();

        if(selectedTopicOne.equalsIgnoreCase("medicine"))
        {
            if(selectedTopicTwo.equalsIgnoreCase("Diseases and disorders"))
            {
                availableTopicThrees.add(new SelectItem("Signs and symptoms"));
                availableTopicThrees.add(new SelectItem("Diagnostic techniques"));
                availableTopicThrees.add(new SelectItem("Therapies"));
                availableTopicThrees.add(new SelectItem("Drugs"));
            }
            else if(selectedTopicTwo.equalsIgnoreCase("Anatomy"))
            {
                availableTopicThrees.add(new SelectItem("Organs"));
                availableTopicThrees.add(new SelectItem("Bones"));
                availableTopicThrees.add(new SelectItem("Muscles"));
                availableTopicThrees.add(new SelectItem("Other"));
            }
        }
        if(selectedTopicOne.equalsIgnoreCase("TOURISM"))
        {
            if(selectedTopicTwo.equalsIgnoreCase("Heritage"))
            {
                availableTopicThrees.add(new SelectItem("History"));
                availableTopicThrees.add(new SelectItem("Architecture"));
                availableTopicThrees.add(new SelectItem("Festivals"));
            }
        }
    }

    public void postProcessXls(Object document)
    {
        try
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
            anchor.setAnchorType(ClientAnchor.DONT_MOVE_AND_RESIZE);

            if(row0 != null)
            {
                cell0 = row0.getCell(0);
            }
            else
            {

                return;
            }

            int i = 2;
            for(i = 2; i <= sheet.getLastRowNum(); i++)
            {
                HSSFRow row = sheet.getRow(i);
                HSSFCell cell = row.getCell(0);

                if(cell != null)
                {
                    if(cell.getStringCellValue().equals(cell0.getStringCellValue()))
                    {
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

            Picture pict = drawing.createPicture(anchor, pictureIdx);
            HSSFCellStyle copyrightStyle = wb.createCellStyle();
            copyrightStyle.setLocked(true);
            sheet.protectSheet("learnweb");
            watermark.delete();

        }
        catch(Exception e)
        {
            log.error("Error in postprocessing Glossary xls for resource: " + resourceId, e);
        }

    }

    public File text2Image(String textString) throws BadElementException, MalformedURLException, IOException
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

    public void postProcessPDF(Object document) throws IOException, DocumentException, SQLException
    {

        Document pdf = (Document) document;
        //Owner or creator of exported resource
        User fileowner = getLearnweb().getResourceManager().getResource(resourceId).getUser();
        //create a File Object
        File file = new File("./" + fileowner.getUsername() + ".jpeg");
        //create the font you wish to use
        Font font = new Font("Tahoma", Font.PLAIN, 18);

        //create the FontRenderContext object which helps us to measure the text
        FontRenderContext frc = new FontRenderContext(null, true, true);

        //get the height and width of the text
        Rectangle2D bounds = font.getStringBounds(fileowner.getUsername(), frc);
        int width = (int) bounds.getWidth();
        int height = (int) bounds.getHeight();

        //create a BufferedImage object
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

        //calling createGraphics() to get the Graphics2D
        Graphics2D graphic = image.createGraphics();

        //set color and other parameters
        graphic.setBackground(Color.white);
        graphic.fillRect(0, 0, width, height);
        graphic.setColor(Color.LIGHT_GRAY);
        graphic.setFont(font);
        graphic.drawString(fileowner.getUsername(), (float) bounds.getX(), (float) -bounds.getY());

        //releasing resources
        graphic.dispose();

        //creating the file
        ImageIO.write(image, "jpeg", file);

        String logo = file.getAbsolutePath();
        Image watermark = Image.getInstance(logo);
        watermark.scaleToFit(400, 400);
        watermark.setAbsolutePosition(100, 500);

        pdf.add(watermark);

    }

    public void changeTopicTwo(AjaxBehaviorEvent event)
    {
        createAvailableTopicsThree();
        selectedTopicThree = null;
    }

    // cached values

    public String getDescription()
    {
        return description;
    }

    public void setDescription(String description)
    {
        this.description = description;
    }

    public List<LanguageItem> getSecondaryLangItems()
    {
        return secondaryLangItems;
    }

    public void setSecondaryLangItems(List<LanguageItem> itItems)
    {
        this.secondaryLangItems = itItems;
    }

    public List<LanguageItem> getPrimaryLangItems()
    {
        return primaryLangItems;
    }

    public void setPrimaryLangItems(List<LanguageItem> ukItems)
    {
        this.primaryLangItems = ukItems;
    }

    public String getSelectedTopicTwo()
    {
        return selectedTopicTwo;
    }

    public void setSelectedTopicTwo(String selectedTopicTwo)
    {
        this.selectedTopicTwo = selectedTopicTwo;
    }

    public String getSelectedTopicOne()
    {
        return selectedTopicOne;
    }

    public void setSelectedTopicOne(String selectedTopicOne)
    {
        this.selectedTopicOne = selectedTopicOne;
        setPreference(PREFERENCE_TOPIC1, selectedTopicOne);
    }

    public String getSelectedTopicThree()
    {
        return selectedTopicThree;
    }

    public void setSelectedTopicThree(String selectedTopicThree)
    {
        this.selectedTopicThree = selectedTopicThree;
    }

    public List<SelectItem> getAvailableTopicOnes()
    {
        return availableTopicOnes;
    }

    public List<SelectItem> getAvailableTopicTwos()
    {
        return availableTopicTwos;
    }

    public List<SelectItem> getAvailableTopicThrees()
    {
        return availableTopicThrees;
    }

    public List<String> getUses()
    {
        return uses;
    }

    public String getValueHeaderIt()
    {
        return valueHeaderIt;
    }

    public int getCount()
    {
        return count;
    }

    public void setCount(int count)
    {
        this.count = count;
    }

    public String getFileName()
    {
        return fileName;
    }

    public void setFileName(String fileName)
    {
        this.fileName = fileName;
    }

    public int getUserId()
    {
        return userId;
    }

    public void setUserId(int userId)
    {
        this.userId = userId;
    }

    public int getResourceId()
    {
        return resourceId;
    }

    public void setResourceId(int resourceId)
    {

        this.resourceId = resourceId;
    }

    public int getGlossaryId()
    {
        return glossaryId;
    }

    public void setGlossaryId(int glossaryId)
    {
        this.glossaryId = glossaryId;
    }

    public GlossaryItems getSelectedGlossaryItem()
    {
        return selectedGlossaryItem;
    }

    public void setSelectedGlossaryItem(GlossaryItems selectedGlossaryItem)
    {

        this.selectedGlossaryItem = selectedGlossaryItem;
    }

    public List<LanguageItem> getLanguageItems()
    {
        return languageItems;
    }

    public void setLanguageItems(List<LanguageItem> languageItems)
    {
        this.languageItems = languageItems;
    }

    private void getGlossaryItems(int id) throws SQLException
    {
        items = getLearnweb().getGlossariesManager().getGlossaryItems(id);
    }

    public List<GlossaryItems> getItems()
    {
        return items;
    }

    public void setItems(List<GlossaryItems> items)
    {
        this.items = items;
    }

    public List<GlossaryItems> getFilteredItems()
    {
        return filteredItems;
    }

    public void setFilteredItems(List<GlossaryItems> filteredItems)
    {
        this.filteredItems = filteredItems;
    }

    public LANGUAGE getPrimaryLanguage()
    {
        return primaryLanguage;
    }

    public void setPrimaryLanguage(LANGUAGE primaryLanguage)
    {
        this.primaryLanguage = primaryLanguage;
    }

    public LANGUAGE getSecondaryLanguage()
    {
        return secondaryLanguage;
    }

    public void setSecondaryLanguage(LANGUAGE secondaryLanguage)
    {
        this.secondaryLanguage = secondaryLanguage;
    }

    public boolean isDeleted()
    {
        return deleted;
    }

    public void setDeleted(boolean deleted)
    {
        this.deleted = deleted;
    }

    public int getGlossaryEntryCount()
    {
        return glossaryEntryCount;
    }

    public boolean isPaginatorActive()
    {
        return paginatorActive;
    }

    public void setPaginatorActive(boolean paginatorActive)
    {
        this.paginatorActive = paginatorActive;
    }

}
