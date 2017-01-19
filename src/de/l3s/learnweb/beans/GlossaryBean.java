package de.l3s.learnweb.beans;
import java.sql.SQLException;
import java.util.Date;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.event.AjaxBehaviorEvent;
import javax.faces.model.SelectItem;
import javax.faces.bean.SessionScoped;

import org.primefaces.model.UploadedFile;

import de.l3s.glossary.ItalianItem;
import de.l3s.glossary.UkItem;
import de.l3s.learnwebBeans.ApplicationBean;

@ViewScoped
@ManagedBean
public class GlossaryBean extends ApplicationBean
{

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

    public void addIt()
    {

	ItalianItems.add(new ItalianItem());
	count++;
	valueHeaderIt = "Term It" + Integer.toString(count);



    }

    public void removeIt(ItalianItem item)
    {

	ItalianItems.remove(ItalianItems.size() - 1);

    }

    public void removeUk(UkItem item)
    {

	UkItems.remove(UkItems.size() - 1);
    }



    public void addUk()
    {
	UkItems.add(new UkItem());

    }

    public void changeTopicOne(AjaxBehaviorEvent event)
    {
	availableTopicTwos.add(new SelectItem("Diseases and disorders"));
	availableTopicTwos.add(new SelectItem("Anatomy"));
	availableTopicTwos.add(new SelectItem("Medical branches"));
	availableTopicTwos.add(new SelectItem("Instituion"));
	availableTopicTwos.add(new SelectItem("Profession"));
	selectedTopicTwo = selectedTopicThree = null;
	availableTopicThrees.clear();
    }

    public void changeTopicTwo(AjaxBehaviorEvent event)
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
	else
	{
	    availableTopicThrees = null;
	}
	selectedTopicThree = null;
    }


    public List<ItalianItem> getItalianItems()
    {
	return ItalianItems;

    }

    public List<UkItem> getUkItems()
    {

	return UkItems;

    }

    public void setUkItems(List<UkItem> ukItems)
    {

	UkItems = ukItems;

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
