package de.l3s.learnweb.beans;

import java.sql.SQLException;
import java.util.Date;
import java.util.List;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;

import de.l3s.learnweb.Glossary;
import de.l3s.learnweb.Learnweb;
import de.l3s.learnwebBeans.ApplicationBean;

@ManagedBean
@ViewScoped
public class GlossaryBean extends ApplicationBean
{

    private Glossary selectedEntry = new Glossary();

    private List<Glossary> entries;
    private int resourceId = 0;

    public GlossaryBean() throws SQLException
    {
	Learnweb lw = getLearnweb();
	//lw.getGlossaryManager();
	//GlossaryManager gm = new GlossaryManager(lw);
	entries = lw.getGlossaryManager().getGlossaryByResourceId(this.resourceId);
    }

    public List<Glossary> getEntries()
    {
	return entries;
    }

    public String edit(Glossary entry)
    {
	selectedEntry = entry;

	return "editGlossary.xhtml";
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

	return "showGlossary.xhtml";
    }

    public String deleteEntry(Glossary entry) throws SQLException
    {

	Learnweb lw = getLearnweb();
	lw.getGlossaryManager().delete(entry.getId());

	return "showGlossary.xhtml";
    }

    public String quit()
    {
	return "showGlossary.xhtml";
    }

    public Glossary getSelectedEntry()
    {
	return selectedEntry;
    }

    public void setSelectedEntry(Glossary selectedEntry)
    {
	this.selectedEntry = selectedEntry;
    }

}
