package de.l3s.learnweb.beans;

import java.io.Serializable;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.context.FacesContext;
import javax.faces.event.AjaxBehaviorEvent;
import javax.faces.model.SelectItem;

import org.apache.log4j.Logger;
import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.primefaces.model.UploadedFile;

import de.l3s.glossary.GlossaryItems;
import de.l3s.glossary.LanguageItem;
import de.l3s.glossary.LanguageItem.LANGUAGE;
import de.l3s.learnweb.GlossaryEntry;
import de.l3s.learnweb.LogEntry.Action;
import de.l3s.learnweb.Resource;

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
    public String description;
    private final List<SelectItem> availableTopicOnes = new ArrayList<SelectItem>();
    private List<SelectItem> availableTopicTwos = new ArrayList<SelectItem>();
    private List<SelectItem> availableTopicThrees = new ArrayList<SelectItem>();
    private String valueHeaderIt;
    private int count;
    private int glossaryEntryCount;
    private UploadedFile multimediaFile;
    private int userId;
    private LANGUAGE primaryLanguage;
    private LANGUAGE secondaryLanguage;
    private int resourceId;
    private int groupId; // group id of the resource used only for the logger
    private int glossaryId;
    private List<GlossaryItems> items = new ArrayList<GlossaryItems>();
    private List<GlossaryItems> filteredItems = new ArrayList<GlossaryItems>();
    private int numberOfEntries;

    private GlossaryItems selectedGlossaryItem;

    public void preRenderView()
    {
        if(isAjaxRequest())
            return;

        if(resourceId > 0 && !getLearnweb().getGlossariesManager().checkIfExists(resourceId))
        {
            getGlossaryItems(resourceId);
            setlanguagePair(resourceId);
            setFilteredItems(getItems());
            glossaryEntryCount = getGlossaryEntryCount(resourceId);
            try
            {
                Resource resource = getLearnweb().getResourceManager().getResource(resourceId);
                groupId = resource.getGroupId();
                log(Action.glossary_open, groupId, resourceId);
            }
            catch(Exception e)
            {
                log.error("Couldn't log glossary action; resource: " + resourceId);
            }

        }
    }

    @PostConstruct
    public void init()
    {
        //Add topic One
        availableTopicOnes.add(new SelectItem("Environment"));
        availableTopicOnes.add(new SelectItem("European Politics"));
        availableTopicOnes.add(new SelectItem("Medicine"));
        availableTopicOnes.add(new SelectItem("Tourism"));

        uses.add("technical");
        uses.add("popular");
        uses.add("informal");
        try
        {
            resourceId = getParameterInt("resource_id");
        }
        catch(Exception e)
        {
            resourceId = 0;
        }

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
            entry.setSelectedTopicOne(getSelectedTopicOne());
            entry.setSelectedTopicTwo(getSelectedTopicTwo());
            entry.setSelectedTopicThree(getSelectedTopicThree());
            entry.setFirstLanguageItems(getPrimaryLangItems());
            entry.setUser(getUser());
            entry.setSecondLanguageItems(getSecondaryLangItems());
            entry.setResourceId(getResourceId());
            entry.setGlossaryId(getGlossaryId());

            boolean result = getLearnweb().getGlossariesManager().addToDatabase(entry);
            if(result)
            {
                FacesContext context = FacesContext.getCurrentInstance();
                context.addMessage(null, new FacesMessage("Successful entry"));
                context.getExternalContext().getFlash().setKeepMessages(true);
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
            FacesContext context = FacesContext.getCurrentInstance();

            context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Please enter atleast one valid entry for both language terms", ""));
            context.getExternalContext().getFlash().setKeepMessages(true);
            return "/lw/showGlossary.jsf?resource_id=" + Integer.toString(getResourceId()) + "&faces-redirect=true";
        }

    }

    public String delete(GlossaryItems item)
    {
        try
        {
            getLearnweb().getGlossariesManager().deleteFromDb(item.getGlossId());
            createEntry();
            log(Action.glossary_entry_delete, groupId, resourceId, Integer.toString(item.getGlossId()));
        }
        catch(Exception e)
        {
            addFatalMessage(e);
        }
        return "/lw/showGlossary.jsf?resource_id=" + Integer.toString(getResourceId()) + "&faces-redirect=true";
    }

    // TODO rename
    public void addIt()
    {
        secondaryLangItems.add(new LanguageItem());
        count++;
        valueHeaderIt = "Term Two" + Integer.toString(count);

        log(Action.glossary_term_add, groupId, resourceId);
    }

    // TODO rename
    public void removeIt(LanguageItem item)
    {
        try
        {
            List<LanguageItem> iItems = new ArrayList<LanguageItem>(secondaryLangItems);
            boolean remove = false;
            for(LanguageItem i : iItems)
            {
                if(!i.getValue().isEmpty())
                    if(iItems.size() > 1)
                        remove = true;
            }
            if(remove)
            {
                secondaryLangItems.remove(item);

                log(Action.glossary_term_delete, groupId, resourceId, Integer.toString(item.getTermId()));
            }
            else
            {
                addMessage(FacesMessage.SEVERITY_ERROR, "You need atleast one entry of Second Language Terms");
            }
        }
        catch(Exception e)
        {
            addFatalMessage(e);
        }
    }

    // TODO rename
    public void removeUk(LanguageItem item)
    {
        try
        {
            List<LanguageItem> primaryItems = new ArrayList<LanguageItem>(primaryLangItems);
            boolean remove = false;
            for(LanguageItem u : primaryItems)
            {
                if(!u.getValue().isEmpty())
                    if(primaryItems.size() > 1)
                        remove = true;
            }
            if(remove)
            {
                primaryLangItems.remove(item);

                log(Action.glossary_term_delete, groupId, resourceId, Integer.toString(item.getTermId()));
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

    // TODO rename
    public void addUk()
    {
        try
        {
            primaryLangItems.add(new LanguageItem());

            log(Action.glossary_term_add, groupId, resourceId, "");
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
        }
        catch(Exception e)
        {
            log.error("Error in postprocessing Glossary xls for resource: " + resourceId, e);
        }

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

    public UploadedFile getMultimediaFile()
    {
        return multimediaFile;
    }

    public void setMultimediaFile(UploadedFile multimediaFile)
    {
        this.multimediaFile = multimediaFile;
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

    private void getGlossaryItems(int id)
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

    public int getNumberOfEntries()
    {
        return numberOfEntries;
    }

    public int getGlossaryEntryCount()
    {
        return glossaryEntryCount;
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

}
