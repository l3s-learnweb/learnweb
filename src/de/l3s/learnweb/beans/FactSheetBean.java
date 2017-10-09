package de.l3s.learnweb.beans;

import java.io.Serializable;
import java.text.ParseException;
import java.util.Random;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;

import org.apache.log4j.Logger;

import de.l3s.learnweb.facts.Entity;
import de.l3s.learnweb.facts.FactSheetEntry;
import de.l3s.learnweb.facts.Search;

@ManagedBean
@ViewScoped
public class FactSheetBean extends ApplicationBean implements Serializable
{
    private String search;
    private Entity entity;

    public String getSearch()
    {
        return search;
    }

    public void setSearch(String search)
    {
        this.search = search;
    }

    public String getRandom()
    {
        String back;
        Random randomGenerator = new Random();
        int random = randomGenerator.nextInt(1000);
        back = "Q" + random;
        return back;
    }

    public void onClick() throws ParseException
    {
        // todo translate query to id

        String language = UtilBean.getUserBean().getLocaleAsString().substring(0, 2);

        Logger.getLogger(FactSheetBean.class).debug(language + " query " + search);

        entity = Search.searchRdfWikidata(search, language);

        for(FactSheetEntry fact : entity.getFacts())
        {
            // replace strange label 
            if(fact.getLabel().equals("mass"))
                fact.setLabel(getLocaleMessage("weight"));
        }
    }

    public Entity getEntity()
    {
        return entity;
    }
}
