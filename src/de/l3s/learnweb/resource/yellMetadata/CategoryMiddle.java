package de.l3s.learnweb.resource.yellMetadata;

import de.l3s.learnweb.Learnweb;
import de.l3s.learnweb.resource.Resource;

import java.io.Serializable;
import java.sql.SQLException;
import java.util.List;

public class CategoryMiddle implements Comparable<CategoryMiddle>, Serializable, CategoryInterface
{
    private static final long serialVersionUID = -3539397525937561097L;

    private int id;
    private String catName;
    private int catTopId;

    public CategoryMiddle()
    {

    }

    public CategoryMiddle(int id, String catName, int catTopId)
    {
        this.id = id;
        this.catName = catName;
        this.catTopId = catTopId;
    }

    public int getId()
    {
        return id;
    }

    public void setId(int id)
    {
        this.id = id;
    }

    public String getCatName()
    {
        return catName;
    }

    public void setCatName(String catName)
    {
        this.catName = catName;
    }

    public int getCatTopId()
    {
        return catTopId;
    }

    public void setCatTopId(int catTopId)
    {
        this.catTopId = catTopId;
    }

    public List<Resource> getResources() throws SQLException
    {
        return Learnweb.getInstance().getResourceManager().getResourcesByCatmidId(id);
    }

    @Override
    public int compareTo(CategoryMiddle o)
    {
        return this.getCatName().compareTo(o.getCatName());
    }

}
