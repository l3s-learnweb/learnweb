package de.l3s.learnweb.beans;

import java.util.LinkedList;
import java.util.List;

import javax.faces.bean.ManagedBean;
import javax.faces.view.ViewScoped;

@ManagedBean
@ViewScoped
public class FactSheetBean
{
    private List<FactSheetEntry> facts;
    String search;

    public String getSearch()
    {
	return search;
    }

    public void setSearch(String search)
    {
	this.search = search;
    }

    public List<FactSheetEntry> getFacts()
    {
	return facts;
    }

    public void onClick()
    {
	String[] parents = { "marta", "hilde" };

	facts = new LinkedList<>();
	facts.add(new FactSheetEntry("parents", parents, "list"));

    }

    public class FactSheetEntry
    {
	public String label;
	public String[] data;
	public String template;

	public FactSheetEntry(String label, String[] data, String template)
	{
	    super();
	    this.label = label;
	    this.data = data;
	    this.template = template;
	}

	public String getLabel()
	{
	    return label;
	}

	public String[] getData()
	{
	    return data;
	}

	public String getTemplate()
	{
	    return template;
	}

    }

}
