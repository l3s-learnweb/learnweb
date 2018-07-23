package de.l3s.learnweb.resource.yellMetadata;

import de.l3s.learnweb.Learnweb;
import de.l3s.learnweb.resource.Resource;

import java.io.Serializable;
import java.sql.SQLException;
import java.util.List;

public class CategoryBottom implements Comparable<CategoryBottom>, Serializable, CategoryInterface
{
    private static final long serialVersionUID = 5670585204494899758L;

    private int id;
    private String catName;
    private int catMidId;

    public CategoryBottom()
    {

    }

    public CategoryBottom(int id, String catName, int catMidId)
    {
        this.id = id;
        this.catName = catName;
        this.catMidId = catMidId;
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

    public int getCatMidId()
    {
        return catMidId;
    }

    public void setCatMidId(int catMidId)
    {
        this.catMidId = catMidId;
    }

    public List<Resource> getResources() throws SQLException
    {
        return Learnweb.getInstance().getResourceManager().getResourcesByCatbotId(id);
    }

    @Override
    public int compareTo(CategoryBottom o)
    {
        return this.getCatName().compareTo(o.getCatName());
    }

}
