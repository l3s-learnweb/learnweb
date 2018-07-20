package de.l3s.learnweb.resource.glossaryNew;

import java.io.File;
import java.io.Serializable;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import javax.inject.Named;
import javax.faces.view.ViewScoped;
import javax.faces.event.AjaxBehaviorEvent;
import javax.faces.model.SelectItem;

import org.apache.log4j.Logger;

import de.l3s.learnweb.LogEntry.Action;
import de.l3s.learnweb.beans.ApplicationBean;
import de.l3s.learnweb.user.User;
import de.l3s.util.Misc;

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
    @Deprecated
    private List<GlossaryTerm> formTerms; // TODO use formEntry.getTerms() instead
    private final List<SelectItem> availableTopicOne = new ArrayList<>();

    private List<SelectItem> availableTopicTwo = new ArrayList<>();
    private List<SelectItem> availableTopicThree = new ArrayList<>();

    public void onLoad() throws SQLException
    {

        User user = getUser();
        if(user == null)
            return;

        glossaryResource = getLearnweb().getGlossaryManager().getGlossaryResource(resourceId);

        /*if(glossaryResource == null)
        {
            addInvalidParameterMessage("resource_id");
            return;
        }*/
        availableTopicOne.add(new SelectItem("Environment"));
        availableTopicOne.add(new SelectItem("European Politics"));
        availableTopicOne.add(new SelectItem("Medicine"));
        availableTopicOne.add(new SelectItem("Tourism"));
        log(Action.glossary_open, glossaryResource);
        loadGlossaryTable(glossaryResource);
        setFilteredTableItems(tableItems);
        setNewFormEntry();

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

    public void setNewFormEntry()
    {
        setFormEntry(new GlossaryEntry());
        setFormTerms(new ArrayList<>());
        formTerms.add(new GlossaryTerm());
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
        log(Action.glossary_term_add, glossaryResource); // should store resourceId + term_id

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
        // return glossaryResource.getEntries().size();
        return 0;
    }

    public GlossaryEntry getFormEntry()
    {
        return formEntry;
    }

    public void setFormEntry(GlossaryEntry formEntry) // TODO necessary?
    {
        this.formEntry = formEntry;
    }

    @Deprecated
    public List<GlossaryTerm> getFormTerms()
    {
        return formTerms;
    }

    @Deprecated
    public void setFormTerms(List<GlossaryTerm> formTerms) // TODO necessary?
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

    public void setAvailableTopicTwo(List<SelectItem> availableTopicTwo) // TODO necessary?
    {
        this.availableTopicTwo = availableTopicTwo;
    }

    public List<SelectItem> getAvailableTopicThree()
    {
        return availableTopicThree;
    }

    public void setAvailableTopicThree(List<SelectItem> availableTopicThree) // TODO necessary?
    {
        this.availableTopicThree = availableTopicThree;
    }

    private transient List<SelectItem> availableLanguages;

    public List<SelectItem> getAvailableLanguages()
    {
        if(null == availableLanguages)
        {
            availableLanguages = new ArrayList<>();

            for(Locale locale : getUser().getOrganisation().getGlossaryLanguages()) // TODO use glossaryResource.getAllowedLanguages() instead
            {
                log.debug("add locales " + locale.getLanguage());
                availableLanguages.add(new SelectItem(locale, getLocaleMessage("language_" + locale.getLanguage())));
            }
            availableLanguages.sort(Misc.selectItemLabelComparator);
        }
        return availableLanguages;
    }

}
