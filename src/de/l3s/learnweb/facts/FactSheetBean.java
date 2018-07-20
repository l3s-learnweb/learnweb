package de.l3s.learnweb.facts;

import java.io.Serializable;
import java.text.ParseException;
import java.util.Random;

import javax.inject.Named;
import javax.faces.view.ViewScoped;

import org.apache.log4j.Logger;

import de.l3s.learnweb.beans.ApplicationBean;
import de.l3s.learnweb.beans.UtilBean;

@Named
@ViewScoped
public class FactSheetBean extends ApplicationBean implements Serializable
{
    private static final long serialVersionUID = -5464782129571454914L;
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

        String language = UtilBean.getUserBean().getLocaleCode();

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
