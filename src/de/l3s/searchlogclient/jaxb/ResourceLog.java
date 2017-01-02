package de.l3s.searchlogclient.jaxb;

import java.io.Serializable;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "ResourceLog")
public class ResourceLog implements Serializable
{
    private static final long serialVersionUID = -4878996100412817963L;
    private int userId;
    private int resultsetId;
    private String action;
    private String timestamp;
    private String url;

    //For QueryHistory Object
    private int resourceRank;
    private String filename;
    private String source;

    private long viewTime;
    private String actionTime;

    public ResourceLog()
    {
    }

    public ResourceLog(int userId, int resultsetId, String action, String timestamp, String url)
    {
        this.userId = userId;
        this.resultsetId = resultsetId;
        this.action = action;
        this.timestamp = timestamp;
        this.url = url;

    }

    public ResourceLog(int userId, int resultsetId, int resourceRank, String action, String timestamp, String url)
    {
        this.userId = userId;
        this.resultsetId = resultsetId;
        this.resourceRank = resourceRank;
        this.action = action;
        this.timestamp = timestamp;
        this.url = url;

    }

    public ResourceLog(int userId, int resultsetId, int resourceRank, String action, String actionTimestamp, String url, String filename, String source)
    {
        this.userId = userId;
        this.resultsetId = resultsetId;
        this.resourceRank = resourceRank;
        this.action = action;
        this.timestamp = actionTimestamp;
        this.url = url;
        this.filename = filename;
        this.source = source;
    }

    public ResourceLog(int userId, int resultsetId, int resourceRank, String action, String actionTimestamp, String actionTime, String url, String filename, String source)
    {
        this.userId = userId;
        this.resultsetId = resultsetId;
        this.resourceRank = resourceRank;
        this.action = action;
        this.timestamp = actionTimestamp;
        this.actionTime = actionTime;
        this.url = url;
        this.filename = filename;
        this.source = source;
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
    public int getResultsetId()
    {
        return resultsetId;
    }

    public void setResultsetId(int resultsetId)
    {
        this.resultsetId = resultsetId;
    }

    @XmlElement
    public String getAction()
    {
        return action;
    }

    public void setAction(String action)
    {
        this.action = action;
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
    public String getUrl()
    {
        return url;
    }

    public void setUrl(String url)
    {
        this.url = url;
    }

    @XmlElement
    public int getResourceRank()
    {
        return resourceRank;
    }

    @XmlElement
    public String getFilename()
    {
        return filename;
    }

    public void setResourceRank(int resourceRank)
    {
        this.resourceRank = resourceRank;
    }

    public void setFilename(String filename)
    {
        this.filename = filename;
    }

    @XmlElement
    public String getSource()
    {
        return source;
    }

    public void setSource(String source)
    {
        this.source = source;
    }

    @XmlElement
    public long getViewTime()
    {
        return viewTime;
    }

    public void setViewTime(long viewTime)
    {
        this.viewTime = viewTime;
    }

    @XmlElement
    public String getActionTime()
    {
        return actionTime;
    }

    public void setActionTime(String actionTime)
    {
        this.actionTime = actionTime;
    }

}
