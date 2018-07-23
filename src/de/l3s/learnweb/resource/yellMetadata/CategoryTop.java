package de.l3s.learnweb.resource.yellMetadata;

import de.l3s.learnweb.Learnweb;
import de.l3s.learnweb.resource.Resource;

import java.io.Serializable;
import java.sql.SQLException;
import java.util.List;

public class CategoryTop implements Comparable<CategoryTop>, Serializable, CategoryInterface
{
    private static final long serialVersionUID = -3916250891965668996L;

    private int id;
    private String catName;

    public CategoryTop()
    {

    }

    public CategoryTop(int id, String catName)
    {
        this.id = id;
        this.catName = catName;
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

    public List<Resource> getResources() throws SQLException
    {
        return Learnweb.getInstance().getResourceManager().getResourcesByCattopId(id);
    }

    @Override
    public int compareTo(CategoryTop o)
    {
        return this.getCatName().compareTo(o.getCatName());
    }

}
