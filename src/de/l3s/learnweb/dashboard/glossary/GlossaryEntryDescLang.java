package de.l3s.learnweb.dashboard.glossary;

import de.l3s.learnweb.Learnweb;
import de.l3s.learnweb.user.User;

import java.io.Serializable;
import java.sql.SQLException;

public class GlossaryEntryDescLang implements Serializable
{
    private static final long serialVersionUID = -3538944153978434822L;

    private Integer userId;
    private String description;
    private String lang;
    private Integer length;
    private Integer entryId;

    private transient User user;

    public GlossaryEntryDescLang()
    {
    }

    public User getUser() throws SQLException
    {
        if(null == user && userId > 0)
        {
            user = Learnweb.getInstance().getUserManager().getUser(userId);
        }
        return user;
    }

    public String getDescription()
    {
        return description;
    }

    public void setDescription(String description)
    {
        this.description = description;
        if(description != null && !description.isEmpty())
            this.length = description.split(" ").length;
    }

    public String getLang()
    {
        return lang;
    }

    public void setLang(String lang)
    {
        this.lang = lang;
    }

    public Integer getLength()
    {
        return length;
    }

    public void setLength(Integer length)
    {
        this.length = length;
    }

    public Integer getUserId()
    {
        return userId;
    }

    public void setUserId(Integer userId)
    {
        this.userId = userId;
    }

    public Integer getEntryId()
    {
        return entryId;
    }

    public void setEntryId(Integer entryId)
    {
        this.entryId = entryId;
    }
}
