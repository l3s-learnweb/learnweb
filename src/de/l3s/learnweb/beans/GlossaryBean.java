package de.l3s.learnweb.beans;

import java.io.Serializable;
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
import org.primefaces.model.UploadedFile;

import de.l3s.glossary.GlossaryItems;
import de.l3s.glossary.LanguageItem;
import de.l3s.learnweb.GlossaryEntry;
import de.l3s.learnweb.LogEntry.Action;
import de.l3s.learnweb.Resource;
import de.l3s.learnweb.User;
import de.l3s.learnwebBeans.ApplicationBean;

@ViewScoped
@ManagedBean
public class GlossaryBean extends ApplicationBean implements Serializable
{
    private static final long serialVersionUID = -1811030091337893637L;
    private static final Logger log = Logger.getLogger(GlossaryBean.class);

    private List<LanguageItem> ItalianItems;
    private List<LanguageItem> UkItems;
    private List<LanguageItem> languageItems;
    private List<String> Uses;
    private String fileName;
    private String selectedTopicOne;
    private String selectedTopicTwo;
    private String selectedTopicThree;
    public String description;
    private List<SelectItem> availableTopicOnes;
    private List<SelectItem> availableTopicTwos;
    private List<SelectItem> availableTopicThrees;
    private String valueHeaderIt;
    private int count;
    private UploadedFile multimediaFile;
    private int userId;
    private int resourceId;
    private int groupId; // group id of the resource used only for the logger
    private int glossaryId;
    private List<GlossaryItems> items = new ArrayList<GlossaryItems>();
    private List<GlossaryItems> filteredItems = new ArrayList<GlossaryItems>();

    private GlossaryItems selectedGlossaryItem;

    public void preRenderView()
    {
        if(isAjaxRequest())
            return;

        if(resourceId > 0)
        {
            getGlossaryItems(resourceId);
            setFilteredItems(getItems());

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

        createEntry();

    }

    public void createEntry()
    {

        Uses = new ArrayList<String>();
        Uses.add("technical");
        Uses.add("popular");
        Uses.add("informal");
        setDescription("");
        setSelectedTopicOne("");
        availableTopicOnes = new ArrayList<SelectItem>();
        availableTopicTwos = new ArrayList<SelectItem>();
        availableTopicThrees = new ArrayList<SelectItem>();

        availableTopicOnes.add(new SelectItem("MEDICINE"));
        ItalianItems = new ArrayList<LanguageItem>();
        ItalianItems.add(new LanguageItem());
        UkItems = new ArrayList<LanguageItem>();
        UkItems.add(new LanguageItem());

    }

    public void setForm(GlossaryItems gloss)
    {
        createEntry();
        setSelectedTopicOne(gloss.getTopic_1());
        createAvailableTopicTwos();

        setSelectedTopicTwo(gloss.getTopic_2());

        createAvailableTopicThree(gloss.getTopic_2());
        setSelectedTopicThree(gloss.getTopic_3());
        setDescription(gloss.getDescription());
        List<LanguageItem> ukItemsToSet = new ArrayList<LanguageItem>();
        List<LanguageItem> itItemsToSet = new ArrayList<LanguageItem>();
        for(LanguageItem l : gloss.getFinalItems())
        {

            if(l.getLanguage().equalsIgnoreCase("english"))
            {
                l.setUseLabel("Use");
                l.updateUseLabel();
                ukItemsToSet.add(l);

            }
            else if(l.getLanguage().equalsIgnoreCase("italian"))
            {
                l.setUseLabel("Use");
                l.updateUseLabel();
                itItemsToSet.add(l);
            }
        }
        setItalianItems(itItemsToSet);
        setUkItems(ukItemsToSet);
        setGlossaryId(gloss.getGlossId());

    }

    public String upload()
    {
        boolean upload = false;

        for(LanguageItem uk : getUkItems())
        {
            if(!uk.getValue().isEmpty() && !upload)
            {
                for(LanguageItem it : getItalianItems())
                {
                    if(!it.getValue().isEmpty())
                        upload = true;
                    break;
                }
            }
        }
        if(upload)
        {
            User u = getUser();
            GlossaryEntry entry = new GlossaryEntry();

            entry.setDescription(getDescription());
            //entry.setMultimediaFile(getMultimediaFile());
            // gl.setFileName(getFileName());
            entry.setSelectedTopicOne(getSelectedTopicOne());
            entry.setSelectedTopicTwo(getSelectedTopicTwo());
            entry.setSelectedTopicThree(getSelectedTopicThree());
            entry.setUkItems(getUkItems());
            entry.setUserId(getUserId());
            entry.setUser(u);
            entry.setItalianItems(getItalianItems());
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

            return "/lw/showGlossary.jsf?resource_id=" + Integer.toString(getResourceId()) + "&faces-redirect=true";
        }
        else
        {
            FacesContext context = FacesContext.getCurrentInstance();

            context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error!", "Please enter atleast one valid entry for both Italian and UK items"));
            return "/lw/showGlossary.jsf?resource_id=" + Integer.toString(getResourceId());
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

    public void addIt()
    {
        ItalianItems.add(new LanguageItem());
        count++;
        valueHeaderIt = "Term It" + Integer.toString(count);

        log(Action.glossary_term_add, groupId, resourceId);
    }

    public void removeIt(LanguageItem item)
    {
        try
        {
            List<LanguageItem> iItems = new ArrayList<LanguageItem>(ItalianItems);
            boolean remove = false;
            for(LanguageItem i : iItems)
            {
                if(!i.getValue().isEmpty())
                    if(iItems.size() > 1)
                        remove = true;
            }
            if(remove)
            {
                ItalianItems.remove(item);

                log(Action.glossary_term_delete, groupId, resourceId, Integer.toString(item.getTermId()));
            }
            else
            {
                FacesContext context1 = FacesContext.getCurrentInstance();

                context1.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error!", "You need atleast one entry of Italian Items"));
            }
        }
        catch(Exception e)
        {
            addFatalMessage(e);
        }
    }

    public void removeUk(LanguageItem item)
    {
        try
        {
            List<LanguageItem> uItems = new ArrayList<LanguageItem>(UkItems);
            boolean remove = false;
            for(LanguageItem u : uItems)
            {
                if(!u.getValue().isEmpty())
                    if(uItems.size() > 1)
                        remove = true;
            }
            if(remove)
            {
                UkItems.remove(item);

                log(Action.glossary_term_delete, groupId, resourceId, Integer.toString(item.getTermId()));
            }
            else
            {
                FacesContext context1 = FacesContext.getCurrentInstance();

                context1.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error!", "You need atleast one entry of UK Items"));
            }
        }
        catch(Exception e)
        {
            addFatalMessage(e);
        }
    }

    public void addUk()
    {
        try
        {
            UkItems.add(new LanguageItem());

            log(Action.glossary_term_add, groupId, resourceId, "");
        }
        catch(Exception e)
        {
            addFatalMessage(e);
        }
    }

    public void createAvailableTopicTwos()
    {
        availableTopicTwos.add(new SelectItem("Diseases and disorders"));
        availableTopicTwos.add(new SelectItem("Anatomy"));
        availableTopicTwos.add(new SelectItem("Medical branches"));
        availableTopicTwos.add(new SelectItem("Institutions"));
        availableTopicTwos.add(new SelectItem("Professions"));
    }

    public void changeTopicOne(AjaxBehaviorEvent event)
    {
        createAvailableTopicTwos();
        selectedTopicTwo = selectedTopicThree = null;
        availableTopicThrees.clear();
    }

    public void createAvailableTopicThree(String selectedtopicTwo)
    {
        availableTopicThrees = new ArrayList<SelectItem>();
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
        else
        {
            availableTopicThrees = null;
        }
    }

    public void changeTopicTwo(AjaxBehaviorEvent event)
    {
        createAvailableTopicThree(selectedTopicTwo);
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

    public List<LanguageItem> getItalianItems()
    {
        return ItalianItems;
    }

    public void setItalianItems(List<LanguageItem> itItems)
    {
        this.ItalianItems = itItems;
    }

    public List<LanguageItem> getUkItems()
    {
        return UkItems;
    }

    public void setUkItems(List<LanguageItem> ukItems)
    {
        this.UkItems = ukItems;
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
        return Uses;
    }

    public void setUses(List<String> uses)
    {
        Uses = uses;
    }

    public String getValueHeaderIt()
    {
        return valueHeaderIt;
    }

    public void setValueHeaderIt(String valueHeaderIt)
    {
        this.valueHeaderIt = valueHeaderIt;
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

}
