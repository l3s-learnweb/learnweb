package de.l3s.learnweb.resource.glossaryNew;

import java.io.File;
import java.io.Serializable;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.event.AjaxBehaviorEvent;
import javax.faces.model.SelectItem;

import org.apache.log4j.Logger;

import de.l3s.learnweb.LogEntry.Action;
import de.l3s.learnweb.beans.ApplicationBean;
import de.l3s.learnweb.user.User;

@ViewScoped
@ManagedBean
public class GlossaryBeanNEW extends ApplicationBean implements Serializable
{
    private static final long serialVersionUID = 7104637880221636543L;
    private static final Logger log = Logger.getLogger(GlossaryBeanNEW.class);

    private int resourceId;
    private int groupId; // group id of the resource used only for the logger
    private GlossaryResource glossaryResource;
    private List<GlossaryTableView> tableItems;
    private List<GlossaryTableView> filteredTableItems;
    private GlossaryEntry formEntry;
    private List<GlossaryTerm> formTerms = new ArrayList<GlossaryTerm>();
    private final List<SelectItem> availableTopicOne = Arrays.asList(new SelectItem("Environment"), new SelectItem("European Politics"), new SelectItem("Medicine"), new SelectItem("Tourism"));
    private List<SelectItem> availableTopicTwo = new ArrayList<SelectItem>();
    private List<SelectItem> availableTopicThree = new ArrayList<SelectItem>();

    public void onLoad() throws SQLException
    {
        User user = getUser();
        if(user == null)
            return;

        glossaryResource = getLearnweb().getGlossaryManager().getGlossaryResource(resourceId);

        if(glossaryResource == null)
        {
            addInvalidParameterMessage("resource_id");
            return;
        }
        groupId = getLearnweb().getResourceManager().getResource(resourceId).getGroupId();
        log(Action.glossary_open, groupId, resourceId);
        loadGlossaryTable(glossaryResource);
        setFilteredTableItems(tableItems);

    }

    private void loadGlossaryTable(GlossaryResource glossaryResource2)
    {
        // TODO Auto-generated method stub
        //set tableItems
        tableItems = getLearnweb().getGlossaryManager().convertToGlossaryTableView(glossaryResource2);

    }

    public void setGlossaryForm(List<GlossaryTableView> tableItems)
    {
        //TODO:: set entry details and term details along with ids.

    }

    public void onSave(GlossaryEntry entry)
    {
        getLearnweb().getGlossaryManager().saveEntry(entry);
    }

    public void onCancel()
    {
        setFormEntry(new GlossaryEntry());
        setFormTerms(new ArrayList<GlossaryTerm>());
    }

    public void deleteEntry(GlossaryEntry entry)
    {
        //TODO:: delete entry+terms with delete button from table view
    }

    public void deleteTerm(GlossaryTerm term)
    {
        //TODO:: delete terms from form.
        //if termid<0, ignore else delete from db
    }

    public void addTerm()
    {
        formTerms.add(new GlossaryTerm());
        log(Action.glossary_term_add, groupId, resourceId); // should store resourceId + term_id

    }

    public void postProcessXls(Object document)
    {
        //TODO:: postprocess of xls (Check if possible to simplify with primefaces postprocess)
    }

    //not necessary??
    public File text2Image(String textString)
    {
        return null;
        //TODO:: method to get watermark
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

    public int getCount()
    {
        return glossaryResource.getEntries().size();
    }

    public GlossaryEntry getFormEntry()
    {
        return formEntry;
    }

    public void setFormEntry(GlossaryEntry formEntry)
    {
        this.formEntry = formEntry;
    }

    public List<GlossaryTerm> getFormTerms()
    {
        return formTerms;
    }

    public void setFormTerms(List<GlossaryTerm> formTerms)
    {
        this.formTerms = formTerms;
    }

    public List<SelectItem> getAvailableTopicOne()
    {
        return availableTopicOne;
    }

    public List<SelectItem> getAvailableTopicTwo()
    {
        return availableTopicTwo;
    }

    public void setAvailableTopicTwo(List<SelectItem> availableTopicTwo)
    {
        this.availableTopicTwo = availableTopicTwo;
    }

    public List<SelectItem> getAvailableTopicThree()
    {
        return availableTopicThree;
    }

    public void setAvailableTopicThree(List<SelectItem> availableTopicThree)
    {
        this.availableTopicThree = availableTopicThree;
    }

}
