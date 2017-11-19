package de.l3s.learnweb.rm;

import java.io.Serializable;
import java.util.List;

public class CategoryResource implements Serializable
{
    private static final long serialVersionUID = 8747926606454387782L;
    private int resourceId;
    private int topcatId;
    private int midcatId;
    private int botcatId;
    private String topcatName;
    private List<String> midcatNames;
    private List<String> botcatNames;

    public CategoryResource()
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

    public String getTopcatName()
    {
        return topcatName;
    }

    public void setTopcatName(String topcatName)
    {
        this.topcatName = topcatName;
    }

    public List<String> getMidcatNames()
    {
        return midcatNames;
    }

    public void setMidcatNames(List<String> midcatNames)
    {
        this.midcatNames = midcatNames;
    }

    public List<String> getBotcatNames()
    {
        return botcatNames;
    }

    public void setBotcatNames(List<String> botcatNames)
    {
        this.botcatNames = botcatNames;
    }

    public int getTopcatId()
    {
        return topcatId;
    }

    public void setTopcatId(int topcatId)
    {
        this.topcatId = topcatId;
    }

    public int getMidcatId()
    {
        return midcatId;
    }

    public void setMidcatId(int midcatId)
    {
        this.midcatId = midcatId;
    }

    public int getBotcatId()
    {
        return botcatId;
    }

    public void setBotcatId(int botcatId)
    {
        this.botcatId = botcatId;
    }

}
