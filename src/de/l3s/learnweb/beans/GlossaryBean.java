package de.l3s.learnweb.beans;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.context.FacesContext;
import javax.faces.event.AjaxBehaviorEvent;
import javax.faces.model.SelectItem;

import org.primefaces.model.UploadedFile;

import de.l3s.glossary.GlossaryItems;
import de.l3s.glossary.ItalianItem;
import de.l3s.glossary.LanguageItems;
import de.l3s.glossary.UkItem;
import de.l3s.learnweb.GlossariesManager;
import de.l3s.learnweb.Learnweb;
import de.l3s.learnweb.User;
import de.l3s.learnwebBeans.ApplicationBean;

@ViewScoped
@ManagedBean
public class GlossaryBean extends ApplicationBean implements Serializable
{

    private static final long serialVersionUID = -1811030091337893637L;
    private List<ItalianItem> ItalianItems;
    private List<UkItem> UkItems;
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
    private Learnweb learnweb;
    GlossariesManager gl;
    private int resourceId;
    private int glossaryId;

    // cached values
    private transient User user;

    public String getDescription()
    {
        return description;
    }

    public void setDescription(String description)
    {
        this.description = description;

    }

    @PostConstruct
    public void init()
    {
        System.out.println("Inside init() of GlossaryBean which is called on PostConstruct");
        createEntry();

    }

    public void createEntry()
    {
        Uses = new ArrayList<String>();
        Uses.add("technical");
        Uses.add("popular");
        Uses.add("informal");
        availableTopicOnes = new ArrayList<SelectItem>();
        availableTopicTwos = new ArrayList<SelectItem>();
        availableTopicThrees = new ArrayList<SelectItem>();
        availableTopicOnes.add(new SelectItem("MEDICINE"));
        ItalianItems = new ArrayList<ItalianItem>();
        ItalianItems.add(new ItalianItem());
        UkItems = new ArrayList<UkItem>();
        UkItems.add(new UkItem());
    }

    public void setForm(GlossaryItems gloss)
    {
        createEntry();
        setSelectedTopicOne(gloss.getTopic_1());
        createAvailableTopicTwos();

        setSelectedTopicTwo(gloss.getTopic_2());
        System.out.println(gloss.getTopic_2());
        System.out.println(gloss.getTopic_3());
        createAvailableTopicThree(gloss.getTopic_2());
        setSelectedTopicThree(gloss.getTopic_3());
        setDescription(gloss.getDescription());
        List<UkItem> ukItemsToSet = new ArrayList<UkItem>();
        List<ItalianItem> itItemsToSet = new ArrayList<ItalianItem>();
        for(LanguageItems l : gloss.getFinalItems())
        {
            if(l.getLanguage().equalsIgnoreCase("english"))
            {
                UkItem u = new UkItem();
                u.setAcronym(l.getAcronym());
                u.setPhraseology(l.getPhraseology());
                u.setPronounciation(l.getPronounciation());
                u.setReferences(l.getReferences());
                String uses = l.getSelectedUses();
                if(uses.contains(","))
                    u.setSelectedUses(Arrays.asList((l.getSelectedUses().trim().split(", "))));
                else
                {
                    List<String> tempUse = new ArrayList<String>();
                    tempUse.add(uses.trim());
                    u.setSelectedUses(tempUse);
                }
                u.setValue(l.getValue());
                u.setTermId(l.getTermId());
                ukItemsToSet.add(u);

            }
            else if(l.getLanguage().equalsIgnoreCase("italian"))
            {
                ItalianItem i = new ItalianItem();
                i.setAcronym(l.getAcronym());
                i.setPhraseology(l.getPhraseology());
                i.setPronounciation(l.getPronounciation());
                i.setReferences(l.getReferences());
                String uses = l.getSelectedUses();
                if(uses.contains(","))
                    i.setSelectedUses(Arrays.asList((l.getSelectedUses().trim().split(", "))));
                else
                {
                    List<String> tempUse = new ArrayList<String>();
                    tempUse.add(uses.trim());
                    i.setSelectedUses(tempUse);
                }
                i.setValue(l.getValue());
                i.setTermId(l.getTermId());
                itItemsToSet.add(i);
            }
        }
        setItalianItems(itItemsToSet);
        setUkItems(ukItemsToSet);
        setGlossaryId(gloss.getGlossId());

    }

    public String upload()
    {
        boolean upload = false;

        for(UkItem uk : getUkItems())
        {
            if(!uk.getValue().isEmpty() && !upload)
            {
                for(ItalianItem it : getItalianItems())
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
            gl = getLearnweb().getGlossariesManager();
            gl.setDescription(getDescription());
            gl.setMultimediaFile(getMultimediaFile());
            gl.setFileName(getFileName());
            gl.setSelectedTopicOne(getSelectedTopicOne());
            gl.setSelectedTopicTwo(getSelectedTopicTwo());
            gl.setSelectedTopicThree(getSelectedTopicThree());
            gl.setUkItems(getUkItems());
            gl.setUserId(getUserId());
            gl.setUser(u);
            gl.setItalianItems(getItalianItems());
            gl.setResourceId(getResourceId());
            System.out.println("GLossary ID = " + getGlossaryId());
            if(getGlossaryId() > 0)
            {
                gl.addToDatabase(getGlossaryId());
            }
            else
                gl.addToDatabase(0);
            createEntry();

            FacesContext context = FacesContext.getCurrentInstance();

            context.addMessage(null, new FacesMessage("Successful entry"));

            return "/lw/showGlossary.jsf?resource_id=" + Integer.toString(getResourceId()) + "&faces-redirect=true";
        }
        else
        {
            FacesContext context = FacesContext.getCurrentInstance();

            context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error!", "Please enter atleast one Entry for both Italian and UK items"));

            return null;
        }

    }

    public void showMessage()
    {
        FacesContext.getCurrentInstance().addMessage(null, new FacesMessage("Glossary item added"));
    }

    public String update()
    {
        User u = getUser();
        gl = getLearnweb().getGlossariesManager();
        gl.setDescription(getDescription());
        gl.setMultimediaFile(getMultimediaFile());
        gl.setFileName(getFileName());
        gl.setSelectedTopicOne(getSelectedTopicOne());
        gl.setSelectedTopicTwo(getSelectedTopicTwo());
        gl.setSelectedTopicThree(getSelectedTopicThree());
        gl.setUkItems(getUkItems());
        gl.setUserId(getUserId());
        gl.setUser(u);
        gl.setItalianItems(getItalianItems());
        gl.setResourceId(getResourceId());
        gl.addToDatabase(getGlossaryId());
        createEntry();
        return "/lw/showGlossary.jsf?resource_id=" + Integer.toString(getResourceId()) + "&faces-redirect=true";

    }

    public String delete(GlossaryItems item)
    {
        gl = getLearnweb().getGlossariesManager();
        String deleteTerms = "DELETE FROM `lw_resource_glossary_terms` WHERE glossary_id = " + Integer.toString(item.getGlossId());
        String deleteGlossItem = "DELETE FROM `lw_resource_glossary` WHERE glossary_id = " + Integer.toString(item.getGlossId());
        gl.deleteFromDb(deleteTerms, deleteGlossItem);
        createEntry();
        return "/lw/showGlossary.jsf?resource_id=" + Integer.toString(getResourceId()) + "&faces-redirect=true";

    }

    public void addIt()
    {
        System.out.println("Inside addIt()");
        ItalianItems.add(new ItalianItem());
        count++;
        valueHeaderIt = "Term It" + Integer.toString(count);
    }

    public void removeIt(ItalianItem item)
    {
        if(ItalianItems.size() > 1)
            ItalianItems.remove(item);
    }

    public void removeUk(UkItem item)
    {
        if(UkItems.size() > 1)
            UkItems.remove(item);
    }

    public void addUk()
    {
        UkItems.add(new UkItem());
    }

    public void createAvailableTopicTwos()
    {

        availableTopicTwos.add(new SelectItem("Diseases and disorders"));
        availableTopicTwos.add(new SelectItem("Anatomy"));
        availableTopicTwos.add(new SelectItem("Medical branches"));
        availableTopicTwos.add(new SelectItem("Instituion"));
        availableTopicTwos.add(new SelectItem("Profession"));
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

    public List<ItalianItem> getItalianItems()
    {
        return ItalianItems;
    }

    public void setItalianItems(List<ItalianItem> itItems)
    {
        this.ItalianItems = itItems;
    }

    public List<UkItem> getUkItems()
    {
        return UkItems;
    }

    public void setUkItems(List<UkItem> ukItems)
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

    /*@Override
    public User getUser()
    {
        if(user == null)
        {
            try
            {
                user = Learnweb.getInstance().getUserManager().getUser(userId);
            }
            catch(SQLException e)
            {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        return user;
    }
    
    public void setUser(User user)
    {
        this.user = user;
        this.userId = user.getId();
    }
    */
    public int getUserId()
    {
        return userId;
    }

    public void setUserId(int userId)
    {
        this.userId = userId;
        this.user = null;
    }

    @Override
    protected Learnweb getLearnweb()
    {
        if(null == learnweb)
            learnweb = Learnweb.getInstance();
        return learnweb;
    }

    public int getResourceId()
    {
        return resourceId;
    }

    public void setResourceId(int resourceId)
    {
        System.out.println("Setting resourceId within GlossaryBean: " + resourceId);
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

    /*private Glossary selectedEntry = new Glossary();
    private Glossary topics = new Glossary();
    private Glossary newRow = new Glossary();
    
    private List<Glossary> entries;
    private int resourceId = 0;
    
    public GlossaryBean() throws SQLException
    {
    Learnweb lw = getLearnweb();
    //lw.getGlossaryManager();
    //GlossaryManager gm = new GlossaryManager(lw);
    entries = lw.getGlossaryManager().getGlossaryByResourceId(this.resourceId);
    }
    
    private List<Italianitems> items;
    
    public void add()
    {
    items.add(new Italianitems());
    }
    
    public List<Italianitems> getItems()
    {
    return items;
    }
    
    public List<Glossary> getEntries()
    {
    return entries;
    }
    
    public String edit(Glossary entry)
    {
    selectedEntry = entry;
    
    return "editGlossary.xhtml?faces-redirect=true";
    }
    
    public String addNewEntry() throws SQLException
    {
    selectedEntry.setUser(getUser());
    selectedEntry.setLastModified(new Date());
    entries.add(selectedEntry);
    Learnweb lw = getLearnweb();
    lw.getGlossaryManager().save(selectedEntry);
    selectedEntry = new Glossary();
    
    return "showGlossary.xhtml";
    }
    
    public String save() throws SQLException
    {
    selectedEntry.setLastModified(new Date());
    Learnweb lw = getLearnweb();
    lw.getGlossaryManager().save(selectedEntry);
    selectedEntry = new Glossary();
    
    return "showGlossary.xhtml?faces-redirect=true";
    }
    
    public String deleteEntry(Glossary entry) throws SQLException
    {
    
    Learnweb lw = getLearnweb();
    lw.getGlossaryManager().delete(entry.getId());
    entries.remove(entry);
    return "showGlossary.xhtml";
    }
    
    public String quit()
    {
    selectedEntry = new Glossary();
    return "showGlossary.xhtml?faces-redirect=true";
    }
    
    public Glossary getSelectedEntry()
    {
    return selectedEntry;
    }
    
    public void setSelectedEntry(Glossary selectedEntry)
    {
    this.selectedEntry = selectedEntry;
    }
    
    public Glossary getTopics()
    {
    return topics;
    }
    
    public void setTopics(Glossary topics)
    {
    this.topics = topics;
    }
    */
}
