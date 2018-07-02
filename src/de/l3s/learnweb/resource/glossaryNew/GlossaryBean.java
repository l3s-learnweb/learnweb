package de.l3s.learnweb.resource.glossaryNew;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import de.l3s.learnweb.beans.ApplicationBean;
import de.l3s.learnweb.user.User;

public class GlossaryBean extends ApplicationBean implements Serializable
{

    /**
     *
     */
    private static final long serialVersionUID = 7104637880221636543L;
    private int resourceId;
    private int userId; // user who is manipulating glossary entries
    private GlossaryResource glossaryResource;
    private int count;
    private ArrayList<GlossaryTableView> tableItems = new ArrayList<GlossaryTableView>();

    public void onLoad()
    {
        User user = getUser();
        if(user == null)
            return;

        glossaryResource = getLearnweb().getGlossaryManager().getGlossaryResource(resourceId);
        loadGlossaryTable(glossaryResource);
        if(glossaryResource == null)
        {
            addInvalidParameterMessage("resource_id");
            return;
        }

    }

    private void loadGlossaryTable(GlossaryResource glossaryResource2)
    {
        // TODO Auto-generated method stub
        //set tableItems
        tableItems = getLearnweb().getGlossaryManager().getGlossaryItems(glossaryResource2);

    }

    public void setEntryForm(List<GlossaryTableView> tableItems)
    {
        //TODO:: set entry details and term details along with ids.

    }

    public void onSave(GlossaryEntry entry)
    {
        getLearnweb().getGlossaryManager().uploadEntry(entry, resourceId);
    }

    public void deleteEntry(GlossaryEntry entry)
    {
        //TODO:: delete entry+terms with delete button from table view
    }

    public void deleteTerm(GlossaryEntry Term)
    {
        //TODO:: delete terms from form.
        //if termid<0, ignore else delete from db
    }

    public void postProcessXls(Object document)
    {
        //TODO:: postprocess of xls (Check if possible to simplify with primefaces postprocess)
    }

    public File text2Image(String textString)
    {
        return null;
        //TODO:: method to get watermark
    }

    public int getResourceId()
    {
        return resourceId;
    }

    public void setResourceId(int resourceId)
    {
        this.resourceId = resourceId;
    }

    public int getUserId()
    {
        return userId;
    }

    public void setUserId(int userId)
    {
        this.userId = userId;
    }

    public GlossaryResource getGlossaryResource()
    {
        return glossaryResource;
    }

    public ArrayList<GlossaryTableView> getTableItems()
    {
        return tableItems;
    }

    public int getCount()
    {
        count = glossaryResource.getEntries().size();
        return count;
    }

}
