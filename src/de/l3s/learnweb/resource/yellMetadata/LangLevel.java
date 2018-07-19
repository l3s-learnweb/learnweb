package de.l3s.learnweb.resource.yellMetadata;

import java.io.Serializable;
import java.sql.SQLException;
import java.util.List;

import de.l3s.learnweb.Learnweb;
import de.l3s.learnweb.resource.Resource;

public class LangLevel implements Comparable<LangLevel>, Serializable
{
    private static final long serialVersionUID = 595309816629110418L;

    private int id;
    private String langLevelName;

    public LangLevel()
    {

    }

    public LangLevel(int id, String langLevelName)
    {
        this.id = id;
        this.langLevelName = langLevelName;
    }

    public List<Resource> getResources() throws SQLException
    {
        return Learnweb.getInstance().getResourceManager().getResourcesByLangLevelId(id);
    }

    public int getId()
    {
        return id;
    }

    public void setId(int id)
    {
        this.id = id;
    }

    public String getLangLevelName()
    {
        return langLevelName;
    }

    public void setLangLevelName(String langLevelName)
    {
        this.langLevelName = langLevelName;
    }

    @Override
    public int compareTo(LangLevel o)
    {
        return this.getLangLevelName().compareTo(o.getLangLevelName());
    }

}
