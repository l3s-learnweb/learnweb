package de.l3s.learnweb.dashboard.glossary;

import java.io.Serializable;
import java.sql.SQLException;

import de.l3s.learnweb.Learnweb;
import de.l3s.learnweb.user.User;

public class GlossaryUserActivity implements Serializable
{
    private static final long serialVersionUID = 7241101860157505330L;

    private int userId = -1;
    private int totalGlossaries = 0;
    private int totalTerms = 0;
    private int totalReferences = 0;

    private transient User user;

    public GlossaryUserActivity()
    {
    }

    public GlossaryUserActivity(int userId)
    {
        this.userId = userId;
    }

    public User getUser() throws SQLException
    {
        if(null == user && userId > 0)
        {
            user = Learnweb.getInstance().getUserManager().getUser(userId);
        }
        return user;
    }

    public int getUserId()
    {
        return userId;
    }

    public void setUserId(int userId)
    {
        this.userId = userId;
    }

    public int getTotalGlossaries()
    {
        return totalGlossaries;
    }

    public void setTotalGlossaries(int totalGlossaries)
    {
        this.totalGlossaries = totalGlossaries;
    }

    public int getTotalTerms()
    {
        return totalTerms;
    }

    public void setTotalTerms(int totalTerms)
    {
        this.totalTerms = totalTerms;
    }

    public int getTotalReferences()
    {
        return totalReferences;
    }

    public void setTotalReferences(int totalReferences)
    {
        this.totalReferences = totalReferences;
    }
}
