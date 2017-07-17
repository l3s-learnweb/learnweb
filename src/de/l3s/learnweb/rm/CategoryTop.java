package de.l3s.learnweb.rm;

import java.io.Serializable;
import java.sql.SQLException;
import java.util.List;

import de.l3s.learnweb.Learnweb;
import de.l3s.learnweb.Resource;

public class CategoryTop implements Comparable<CategoryTop>, Serializable
{
    private static final long serialVersionUID = -3916250891965668996L;
    private int id;
    private String cattop_name;

    public CategoryTop()
    {

    }

    public CategoryTop(int id, String cattop_name)
    {
        this.id = id;
        this.cattop_name = cattop_name;
    }

    public int getId()
    {
        return id;
    }

    public void setId(int id)
    {
        this.id = id;
    }

    public String getCattop_name()
    {
        return cattop_name;
    }

    public void setCattop_name(String cattop_name)
    {
        this.cattop_name = cattop_name;
    }

    public List<Resource> getResources() throws SQLException
    {
        return Learnweb.getInstance().getResourceManager().getResourcesByCattopId(id);
    }

    @Override
    public int compareTo(CategoryTop o)
    {
        return this.getCattop_name().compareTo(o.getCattop_name());
    }

}
