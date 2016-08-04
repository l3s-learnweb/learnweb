package de.l3s.learnweb.beans;

import java.text.ParseException;
import java.util.List;

import javax.faces.bean.ManagedBean;
import javax.faces.view.ViewScoped;

import de.l3s.learnweb.facts.Entity;
import de.l3s.learnweb.facts.FactSheetEntry;
import de.l3s.learnweb.facts.Search;
import de.l3s.learnwebBeans.ApplicationBean;

@ManagedBean
@ViewScoped
public class FactSheetBean extends ApplicationBean
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

    public void onClick() throws ParseException
    {
	// todo translate querz to id

	String language = UtilBean.getUserBean().getLocaleAsString().substring(0, 2);
	System.out.println(language + " query " + search);
	Entity entity = Search.searchRdfWikidata(search, language);
	facts = entity.getFacts();
    }

}
