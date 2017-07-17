package de.l3s.learnweb.rm;

import java.io.Serializable;
import java.sql.SQLException;
import java.util.List;

import de.l3s.learnweb.Learnweb;
import de.l3s.learnweb.Resource;

public class Purpose implements Comparable<Purpose>, Serializable
{
    private static final long serialVersionUID = -8405454293772229648L;
    private int id;
    private String purpose_name;

    public Purpose()
    {

    }

    public Purpose(int id, String purpose_name)
    {
        this.id = id;
        this.purpose_name = purpose_name;
    }

    public int getId()
    {
        return id;
    }

    public void setId(int id)
    {
        this.id = id;
    }

    public String getPurpose_name()
    {
        return purpose_name;
    }

    public void setPurpose_name(String purpose_name)
    {
        this.purpose_name = purpose_name;
    }

    public List<Resource> getResources() throws SQLException
    {
        return Learnweb.getInstance().getResourceManager().getResourcesByPurposeId(id);
    }

    @Override
    public int compareTo(Purpose o)
    {
        return this.getPurpose_name().compareTo(o.getPurpose_name());
    }

}
