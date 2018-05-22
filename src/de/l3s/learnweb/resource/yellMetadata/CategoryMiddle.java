package de.l3s.learnweb.resource.yellMetadata;

import java.io.Serializable;
import java.sql.SQLException;
import java.util.List;

import de.l3s.learnweb.Learnweb;
import de.l3s.learnweb.resource.Resource;

public class CategoryMiddle implements Comparable<CategoryMiddle>, Serializable
{
    private static final long serialVersionUID = -3539397525937561097L;
    private int id;
    private String catmid_name;
    private int cattop_id;

    public CategoryMiddle()
    {

    }

    public CategoryMiddle(int id, String catmid_name, int cattop_id)
    {
        this.id = id;
        this.catmid_name = catmid_name;
        this.cattop_id = cattop_id;
    }

    public int getId()
    {
        return id;
    }

    public void setId(int id)
    {
        this.id = id;
    }

    public String getCatmid_name()
    {
        return catmid_name;
    }

    public void setCatmid_name(String catmid_name)
    {
        this.catmid_name = catmid_name;
    }

    public int getCattop_id()
    {
        return cattop_id;
    }

    public void setCattop_id(int cattop_id)
    {
        this.cattop_id = cattop_id;
    }

    public List<Resource> getResources() throws SQLException
    {
        return Learnweb.getInstance().getResourceManager().getResourcesByCatmidId(id);
    }

    @Override
    public int compareTo(CategoryMiddle o)
    {
        return this.getCatmid_name().compareTo(o.getCatmid_name());
    }

}
