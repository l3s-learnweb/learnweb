package de.l3s.learnweb.resource.glossaryNew;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import de.l3s.learnweb.beans.ApplicationBean;
import de.l3s.learnweb.user.User;

public class GlossaryBean extends ApplicationBean implements Serializable
{
    private static final long serialVersionUID = 7104637880221636543L;
    private static final Logger log = Logger.getLogger(GlossaryBean.class);

    private int resourceId;
    private GlossaryResource glossaryResource;
    private int count;
    private List<GlossaryTableView> tableItems = new ArrayList<GlossaryTableView>(); // TODO not necessary

    public void onLoad()
    {
        User user = getUser();
        if(user == null)
            return;

        glossaryResource = getLearnweb().getGlossaryManager().getGlossaryResource(resourceId);
        loadGlossaryTable(glossaryResource); // TODO does is make sense here?
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
        tableItems = getLearnweb().getGlossaryManager().convertToGlossaryTableView(glossaryResource2);

    }

    public void setEntryForm(List<GlossaryTableView> tableItems)
    {
        //TODO:: set entry details and term details along with ids.

    }

    public void onSave(GlossaryEntry entry)
    {
        getLearnweb().getGlossaryManager().saveEntry(entry, resourceId);
    }

    public void deleteEntry(GlossaryEntry entry)
    {
        //TODO:: delete entry+terms with delete button from table view
    }

    public void deleteTerm(GlossaryEntry term)
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

    public GlossaryResource getGlossaryResource()
    {
        return glossaryResource;
    }

    public List<GlossaryTableView> getTableItems()
    {
        return tableItems;
    }

    public int getCount()
    {
        count = glossaryResource.getEntries().size(); // TODO doesn't make sense
        return count;
    }

}
