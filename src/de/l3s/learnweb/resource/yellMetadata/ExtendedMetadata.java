package de.l3s.learnweb.resource.yellMetadata;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ExtendedMetadata implements Serializable
{
    private static final long serialVersionUID = 8459836784341456001L;
    private int resourceId;

    //extended metadata for a single resource: author, language are already included in the main resource class
    private List<String> categories;
    private List<String> targets;
    private List<String> purposes;
    private List<String> levels;
    private Map<String, Integer> targetCount = new HashMap<>();
    private Map<String, Integer> purposeCount = new HashMap<>();
    private Map<String, Integer> levelCount = new HashMap<>();
    //private Map<String, Integer> categoryCount = new HashMap();

    private String lCount;
    private List<String> p4Count; //reading, writing, listening and speaking only
    private List<String> pCount; //the rest

    //aggregated list for search filters
    private List<String> authors;
    private List<String> langs;

    public ExtendedMetadata()
    {

    }

    public int getResourceId()
    {
        return resourceId;
    }

    public void setResourceId(int resourceId)
    {
        this.resourceId = resourceId;
    }

    public List<String> getAuthors()
    {
        return authors;
    }

    public void setAuthors(List<String> authors)
    {
        this.authors = authors;
    }

    public List<String> getTargets()
    {
        return targets;
    }

    public void setTargets(List<String> targets)
    {
        this.targets = targets;
    }

    public List<String> getPurposes()
    {
        return purposes;
    }

    public void setPurposes(List<String> purposes)
    {
        this.purposes = purposes;
    }

    public List<String> getLangs()
    {
        return langs;
    }

    public void setLangs(List<String> langs)
    {
        this.langs = langs;
    }

    public List<String> getLevels()
    {
        return levels;
    }

    public void setLevels(List<String> levels)
    {
        this.levels = levels;
    }

    public Map<String, Integer> getTargetCount()
    {
        return targetCount;
    }

    public void setTargetCount(Map<String, Integer> targetCount)
    {
        this.targetCount = targetCount;
    }

    public Map<String, Integer> getPurposeCount()
    {
        return purposeCount;
    }

    public void setPurposeCount(Map<String, Integer> purposeCount)
    {
        this.purposeCount = purposeCount;
    }

    public Map<String, Integer> getLevelCount()
    {
        return levelCount;
    }

    public void setLevelCount(Map<String, Integer> levelCount)
    {
        this.levelCount = levelCount;
    }

    public List<String> getCategories()
    {
        return categories;
    }

    public void setCategories(List<String> categories)
    {
        this.categories = categories;
    }

    public String getlCount()
    {
        return lCount;
    }

    public void setlCount(String lCount)
    {
        this.lCount = lCount;
    }

    public List<String> getP4Count()
    {
        return p4Count;
    }

    public void setP4Count(List<String> p4Count)
    {
        this.p4Count = p4Count;
    }

    public List<String> getpCount()
    {
        return pCount;
    }

    public void setpCount(List<String> pCount)
    {
        this.pCount = pCount;
    }

    @Override
    public String toString()
    {
        return "ExtendedMetadata [categories=" + categories + ", levels=" + levels + ", targets=" + targets + ", purposes=" + purposes + ", levelCount=" + levelCount + ", targetCount=" + targetCount + "]";
    }

}
