package de.l3s.learnweb.rm;

import java.io.Serializable;
import java.sql.SQLException;
import java.util.List;

import de.l3s.learnweb.Learnweb;
import de.l3s.learnweb.Resource;

public class CategoryBottom implements Comparable<CategoryBottom>, Serializable
{
    private static final long serialVersionUID = 5670585204494899758L;
    private int id;
    private String catbot_name;
    private int catmid_id;

    public CategoryBottom()
    {

    }

    public CategoryBottom(int id, String catbot_name, int catmid_id)
    {
        this.id = id;
        this.catbot_name = catbot_name;
        this.catmid_id = catmid_id;
    }

    public int getId()
    {
        return id;
    }

    public void setId(int id)
    {
        this.id = id;
    }

    public String getCatbot_name()
    {
        return catbot_name;
    }

    public void setCatbot_name(String catbot_name)
    {
        this.catbot_name = catbot_name;
    }

    public int getCatmid_id()
    {
        return catmid_id;
    }

    public void setCatmid_id(int catmid_id)
    {
        this.catmid_id = catmid_id;
    }

    public List<Resource> getResources() throws SQLException
    {
        return Learnweb.getInstance().getResourceManager().getResourcesByCatbotId(id);
    }

    @Override
    public int compareTo(CategoryBottom o)
    {
        return this.getCatbot_name().compareTo(o.getCatbot_name());
    }

}
