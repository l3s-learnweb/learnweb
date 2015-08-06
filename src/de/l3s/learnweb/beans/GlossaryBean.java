package de.l3s.learnweb.beans;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;

import de.l3s.learnwebBeans.ApplicationBean;

@ManagedBean
@SessionScoped
public class GlossaryBean extends ApplicationBean
{

    private GlossaryEntry selectedEntry = new GlossaryEntry();

    private List<GlossaryEntry> entries = new ArrayList<GlossaryEntry>();

    public void setEntries(List<GlossaryEntry> entries)
    {
	this.entries = entries;
    }

    public List<GlossaryEntry> getEntries()
    {
	return entries;
    }

    public String edit(GlossaryEntry entry)
    {
	selectedEntry = entry;

	return "editGlossary.xhtml";
    }

    public String addNewEntry()
    {
	selectedEntry.setUser(getUser());

	selectedEntry.setLastModified(new Date());

	entries.add(selectedEntry);

	selectedEntry = new GlossaryEntry();

	return "showGlossary.xhtml";
    }

    public String save()
    {
	selectedEntry.setLastModified(new Date());

	selectedEntry = new GlossaryEntry();

	return "showGlossary.xhtml";
    }

    public String deleteEntry(GlossaryEntry entries)
    {
	getEntries().remove(entries);

	return null;
    }

    public String quit()
    {
	return "showGlossary.xhtml";
    }

    public GlossaryEntry getSelectedEntry()
    {
	return selectedEntry;
    }

    public void setSelectedEntry(GlossaryEntry selectedEntry)
    {
	this.selectedEntry = selectedEntry;
    }

}
