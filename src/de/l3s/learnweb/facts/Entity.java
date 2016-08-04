package de.l3s.learnweb.facts;

import java.util.List;
import java.util.Map;

public class Entity
{
    private String wikiId;
    private String label;
    private String description;
    private String imageUrl;
    private Map<String, List<String>> wikiStats;
    private Map<String, String> propList;
    private List<FactSheetEntry> facts;

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

    public String getImageUrl()
    {
	return imageUrl;
    }

    public void setImageUrl(String imageUrl)
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

}
