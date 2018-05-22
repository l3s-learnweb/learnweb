package de.l3s.learnweb.resource.yellMetadata;

import java.io.Serializable;
import java.sql.SQLException;
import java.util.List;

import de.l3s.learnweb.Learnweb;
import de.l3s.learnweb.resource.Resource;

public class Audience implements Comparable<Audience>, Serializable
{

    private static final long serialVersionUID = 3713936701945755031L;
    private int id;
    private String audience_name;

    public Audience()
    {

    }

    public Audience(int id, String audience_name)
    {
        this.id = id;
        this.audience_name = audience_name;
    }

    public int getId()
    {
        return id;
    }

    public void setId(int id)
    {
        this.id = id;
    }

    public String getAudience_name()
    {
        return audience_name;
    }

    public void setAudience_name(String audience_name)
    {
        this.audience_name = audience_name;
    }

    public List<Resource> getResources() throws SQLException
    {
        return Learnweb.getInstance().getResourceManager().getResourcesByAudienceId(id);
    }

    @Override
    public int compareTo(Audience o)
    {
        return this.getAudience_name().compareTo(o.getAudience_name());
    }
}
