package de.l3s.learnweb.facts;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

public class Entity implements Serializable
{
    private static final long serialVersionUID = -540747321578066221L;

    private String wikiId;
    private String label;
    private String title; // equal to the page title in Wikipedia    
    private String description;
    private List<String> imageUrl;
    private Map<String, List<String>> wikiStats;
    private Map<String, String> propList;
    private List<FactSheetEntry> facts;
    private List<String> instance;

    public String getWikiId()
    {
        return wikiId;
    }

    public void setWikiId(String wikiId)
    {
        this.wikiId = wikiId;
    }

    public String getLabel()
    {
        return label;
    }

    public void setLabel(String label)
    {
        this.label = label;
    }

    public String getDescription()
    {
        return description;
    }

    public void setDescription(String description)
    {
        this.description = description;
    }

    public List<String> getImageUrl()
    {
        return imageUrl;
    }

    public void setImageUrl(List<String> imageUrl)
    {
        this.imageUrl = imageUrl;
    }

    public Map<String, List<String>> getWikiStats()
    {
        return wikiStats;
    }

    public void setWikiStats(Map<String, List<String>> wikiStats)
    {
        this.wikiStats = wikiStats;
    }

    public Map<String, String> getPropList()
    {
        return propList;
    }

    public void setPropList(Map<String, String> propList)
    {
        this.propList = propList;
    }

    public List<FactSheetEntry> getFacts()
    {
        return facts;
    }

    public void setFacts(List<FactSheetEntry> facts)
    {
        this.facts = facts;
    }

    public String getTitle()
    {
        return title;
    }

    public void setTitle(String title)
    {
        this.title = title;
    }

    public List<String> getInstance()
    {
        return instance;
    }

    public void setInstance(List<String> instance)
    {
        this.instance = instance;
    }

}
