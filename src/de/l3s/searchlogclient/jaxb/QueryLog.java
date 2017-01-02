package de.l3s.searchlogclient.jaxb;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "Querylog")
public class QueryLog
{

    private String query;
    private String searchType;
    private int userId;
    private int groupId;
    private String sessionId;
    private String timestamp;
    private int resultsetId;

    //Default Constructor
    public QueryLog()
    {
    }

    //Parameterized Constructor1
    public QueryLog(String query, String searchType, int userId, int groupId, String sessionId, String timestamp)
    {
        this.query = query;
        this.searchType = searchType;
        this.userId = userId;
        this.groupId = groupId;
        this.sessionId = sessionId;
        this.timestamp = timestamp;
    }

    //Parameterized Constructor2
    public QueryLog(String query, String searchType, int userId, int groupId, String sessionId, String timestamp, int resultsetId)
    {
        this.query = query;
        this.searchType = searchType;
        this.userId = userId;
        this.groupId = groupId;
        this.sessionId = sessionId;
        this.timestamp = timestamp;
        this.resultsetId = resultsetId;
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
    public int getUserId()
    {
        return userId;
    }

    public void setUserId(int userId)
    {
        this.userId = userId;
    }

    @XmlElement
    public int getGroupId()
    {
        return groupId;
    }

    public void setGroupId(int groupId)
    {
        this.groupId = groupId;
    }

    @XmlElement
    public String getTimestamp()
    {
        return timestamp;
    }

    public void setTimestamp(String timestamp)
    {
        this.timestamp = timestamp;
    }

    @XmlElement
    public String getSessionId()
    {
        return sessionId;
    }

    public void setSessionId(String sessionId)
    {
        this.sessionId = sessionId;
    }

    @XmlElement
    public String getSearchType()
    {
        return searchType;
    }

    public void setSearchType(String searchType)
    {
        this.searchType = searchType;
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
}
