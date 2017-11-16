package de.l3s.learnweb.rm;

import java.io.Serializable;
import java.util.List;

public class CategoryTree implements Serializable
{
    private static final long serialVersionUID = -453437192920100110L;

    private String[] uniqueTopcats;
    private String[] uniqueMidcats;
    private String[] uniqueBotcats;
    private List<CategoryResource> catResources;

    public CategoryTree()
    {

    }

    public CategoryTree(int[] resourceIds)
    {

        if(resourceIds.length > 0)
        {
            populateCatResources(resourceIds);
        }

    }

    private void populateCatResources(int[] resourceIds)
    {
        // using the given Ids, i call each resource and get its categoryResource

    }

    public String[] getUniqueTopcats()
    {
        return uniqueTopcats;
    }

    public void setUniqueTopcats(String[] uniqueTopcats)
    {
        this.uniqueTopcats = uniqueTopcats;
    }

    public String[] getUniqueMidcats()
    {
        return uniqueMidcats;
    }

    public void setUniqueMidcats(String[] uniqueMidcats)
    {
        this.uniqueMidcats = uniqueMidcats;
    }

    public String[] getUniqueBotcats()
    {
        return uniqueBotcats;
    }

    public void setUniqueBotcats(String[] uniqueBotcats)
    {
        this.uniqueBotcats = uniqueBotcats;
    }

    public List<CategoryResource> getCatResources()
    {
        return catResources;
    }

    public void setCatResources(List<CategoryResource> catResources)
    {
        this.catResources = catResources;
    }

}
