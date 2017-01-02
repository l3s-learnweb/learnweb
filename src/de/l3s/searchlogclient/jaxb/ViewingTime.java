package de.l3s.searchlogclient.jaxb;

import java.util.Date;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "ViewingTime")
public class ViewingTime
{

    private int resultsetId;
    private int resourceRank;
    private String url;
    private Date startTime;
    private Date endTime;
    private String start_time;
    private String end_time;

    public ViewingTime()
    {
    }

    public ViewingTime(int resultsetId, String url, Date startTime, Date endTime)
    {
        this.resultsetId = resultsetId;
        this.url = url;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public ViewingTime(int resultsetId, int resourceRank, Date startTime, Date endTime)
    {
        this.resultsetId = resultsetId;
        this.resourceRank = resourceRank;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public ViewingTime(int resultsetId, String url, String start_time, String end_time)
    {
        this.resultsetId = resultsetId;
        this.url = url;
        this.start_time = start_time;
        this.end_time = end_time;
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
    public int getResourceRank()
    {
        return resourceRank;
    }

    public void setResourceRank(int resourceRank)
    {
        this.resourceRank = resourceRank;
    }

    @XmlElement
    public Date getStartTime()
    {
        return startTime;
    }

    public void setStartTime(Date startTime)
    {
        this.startTime = startTime;
    }

    @XmlElement
    public Date getEndTime()
    {
        return endTime;
    }

    public void setEndTime(Date endTime)
    {
        this.endTime = endTime;
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

    public String getStart_time()
    {
        return start_time;
    }

    public void setStart_time(String start_time)
    {
        this.start_time = start_time;
    }

    public String getEnd_time()
    {
        return end_time;
    }

    public void setEnd_time(String end_time)
    {
        this.end_time = end_time;
    }
}
