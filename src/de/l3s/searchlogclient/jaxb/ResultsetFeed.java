package de.l3s.searchlogclient.jaxb;

import javax.xml.bind.annotation.XmlElement;

import de.l3s.learnweb.User;

public class ResultsetFeed
{
    private int userSharing;
    private int resultsetId;
    private String md5value;
    private String query;
    private String queryTimestamp;
    private User user;

    public ResultsetFeed()
    {
    }

    public ResultsetFeed(int userSharing, int resultsetId, String md5value, String query, String queryTimestamp, User user)
    {
        this.userSharing = userSharing;
        this.resultsetId = resultsetId;
        this.md5value = md5value;
        this.query = query;
        this.queryTimestamp = queryTimestamp;
        this.user = user;
    }

    @XmlElement
    public int getUserSharing()
    {
        return userSharing;
    }

    public void setUserSharing(int userSharing)
    {
        this.userSharing = userSharing;
    }

    @XmlElement
    public int getResultsetId()
    {
        return resultsetId;
    }

    public void setResultsetId(int resultsetId)
    {
        this.resultsetId = resultsetId;
    }

    @XmlElement
    public String getMd5value()
    {
        return md5value;
    }

    public void setMd5value(String md5value)
    {
        this.md5value = md5value;
    }

    @XmlElement
    public String getQuery()
    {
        return query;
    }

    public void setQuery(String query)
    {
        this.query = query;
    }

    @XmlElement
    public String getQueryTimestamp()
    {
        return queryTimestamp;
    }

    public void setQueryTimestamp(String queryTimestamp)
    {
        this.queryTimestamp = queryTimestamp;
    }

    public User getUser()
    {
        return user;
    }

    public void setUser(User user)
    {
        this.user = user;
    }

}
