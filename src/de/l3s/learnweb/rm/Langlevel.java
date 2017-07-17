package de.l3s.learnweb.rm;

import java.io.Serializable;
import java.sql.SQLException;
import java.util.List;

import de.l3s.learnweb.Learnweb;
import de.l3s.learnweb.Resource;

public class Langlevel implements Comparable<Langlevel>, Serializable
{
    private static final long serialVersionUID = 595309816629110418L;
    private int id;
    private String langlevel_name;

    public Langlevel()
    {

    }

    public Langlevel(int id, String langlevel_name)
    {
        this.id = id;
        this.langlevel_name = langlevel_name;
    }

    public List<Resource> getResources() throws SQLException
    {
        return Learnweb.getInstance().getResourceManager().getResourcesByLanglevelId(id);
    }

    public int getId()
    {
        return id;
    }

    public void setId(int id)
    {
        this.id = id;
    }

    public String getLanglevel_name()
    {
        return langlevel_name;
    }

    public void setLanglevel_name(String langlevel_name)
    {
        this.langlevel_name = langlevel_name;
    }

    @Override
    public int compareTo(Langlevel o)
    {
        return this.getLanglevel_name().compareTo(o.getLanglevel_name());
    }

}
