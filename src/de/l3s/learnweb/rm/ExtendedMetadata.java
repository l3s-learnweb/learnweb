package de.l3s.learnweb.rm;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ExtendedMetadata implements Serializable
{
    private static final long serialVersionUID = 8459836784341456001L;
    private int resourceId;

    private List<String> categories;
    private List<String> authors;
    private List<String> mtypes;
    private List<String> sources;
    private List<String> targets;
    private List<String> purposes;
    private List<String> langs;
    private List<String> levels;

    private Map<String, Integer> targetCount = new HashMap();
    private Map<String, Integer> purposeCount = new HashMap();
    private Map<String, Integer> levelCount = new HashMap();
    private Map<String, Integer> categoryCount = new HashMap();

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

    public List<String> getMtypes()
    {
        return mtypes;
    }

    public void setMtypes(List<String> mtypes)
    {
        this.mtypes = mtypes;
    }

    public List<String> getSources()
    {
        return sources;
    }

    public void setSources(List<String> sources)
    {
        this.sources = sources;
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

    public Map getTargetCount()
    {
        return targetCount;
    }

    public void setTargetCount(Map targetCount)
    {
        this.targetCount = targetCount;
    }

    public Map getPurposeCount()
    {
        return purposeCount;
    }

    public void setPurposeCount(Map purposeCount)
    {
        this.purposeCount = purposeCount;
    }

    public Map getLevelCount()
    {
        return levelCount;
    }

    public void setLevelCount(Map levelCount)
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

}
