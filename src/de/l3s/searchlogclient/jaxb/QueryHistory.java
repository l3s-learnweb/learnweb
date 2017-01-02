package de.l3s.searchlogclient.jaxb;

import java.util.ArrayList;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import de.l3s.searchlogclient.Timeline;

@XmlRootElement(name = "QueryHistory")
public class QueryHistory
{

    String query;
    String searchType;
    int userId;
    String queryDate;
    String queryTime;
    String queryTimestamp;
    int resultsetid;
    boolean queryselected;
    ResourceLogHashMap resourcesclicked;
    ResourceLogHashMap resourcesSaved;
    ResourceLogHashMap resClickAndSaved;
    ResourceLogList resourceClickList;
    ResourceLogList resourceSavedList;
    ResourceLogList resClickAndSavedList;
    ArrayList<Timeline> timelines;

    public QueryHistory()
    {
        this.query = "";
        this.searchType = "";
        this.userId = 0;
        this.queryDate = "";
        this.queryTime = "";
        this.queryTimestamp = "";
        this.resultsetid = 0;
        this.queryselected = false;
        resourcesclicked = new ResourceLogHashMap();
        resourcesSaved = new ResourceLogHashMap();
        resClickAndSaved = new ResourceLogHashMap();
        resourceClickList = new ResourceLogList();
        resourceSavedList = new ResourceLogList();
        resClickAndSavedList = new ResourceLogList();
        timelines = new ArrayList<Timeline>();
    }

    public QueryHistory(String query, String searchType, int userId, String queryDate, String queryTime, String queryTimestamp, int resultsetid)
    {
        this.query = query;
        this.searchType = searchType;
        this.userId = userId;
        this.queryDate = queryDate;
        this.queryTime = queryTime;
        this.queryTimestamp = queryTimestamp;
        this.queryselected = false;
        this.resultsetid = resultsetid;
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
    public String getSearchType()
    {
        return searchType;
    }

    public void setSearchType(String searchType)
    {
        this.searchType = searchType;
    }

    @XmlElement
    public String getQueryTime()
    {
        return queryTime;
    }

    public void setQueryTime(String queryTime)
    {
        this.queryTime = queryTime;
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
    public ResourceLogHashMap getResourcesclicked()
    {
        return resourcesclicked;
    }

    public void setResourcesclicked(ResourceLogHashMap resourcesclicked)
    {
        this.resourcesclicked = resourcesclicked;
    }

    @XmlElement
    public ResourceLogHashMap getResourcesSaved()
    {
        return resourcesSaved;
    }

    public void setResourcesSaved(ResourceLogHashMap resourcesSaved)
    {
        this.resourcesSaved = resourcesSaved;
    }

    @XmlElement
    public int getResultsetid()
    {
        return resultsetid;
    }

    public void setResultsetid(int resultsetid)
    {
        this.resultsetid = resultsetid;
    }

    @XmlElement
    public String getQueryDate()
    {
        return queryDate;
    }

    public void setQueryDate(String queryDate)
    {
        this.queryDate = queryDate;
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

    @XmlElement
    public boolean isQueryselected()
    {
        return queryselected;
    }

    public void setQueryselected(boolean queryselected)
    {
        this.queryselected = queryselected;
    }

    @XmlElement
    public ResourceLogList getResourceClickList()
    {
        return resourceClickList;
    }

    public void setResourceClickList(ResourceLogList resourceClickList)
    {
        this.resourceClickList = resourceClickList;
    }

    @XmlElement
    public ResourceLogList getResourceSavedList()
    {
        return resourceSavedList;
    }

    public void setResourceSavedList(ResourceLogList resourceSavedList)
    {
        this.resourceSavedList = resourceSavedList;
    }

    @XmlElement
    public ResourceLogList getResClickAndSavedList()
    {
        return resClickAndSavedList;
    }

    public void setResClickAndSavedList(ResourceLogList resClickAndSavedList)
    {
        this.resClickAndSavedList = resClickAndSavedList;
    }

    public ArrayList<Timeline> getTimelines()
    {
        return timelines;
    }

    public void setTimelines(ArrayList<Timeline> timelines)
    {
        this.timelines = timelines;
    }

    public ResourceLogHashMap getResClickAndSaved()
    {
        return resClickAndSaved;
    }

    public void setResClickAndSaved(ResourceLogHashMap resClickAndSaved)
    {
        this.resClickAndSaved = resClickAndSaved;
    }

    public boolean isResourceClicked(String timestamp)
    {
        return resourcesclicked.getResourceLogHashMap().containsKey(timestamp);
    }

    public ResourceLog getResourceClicked(String timestamp)
    {
        return resourcesclicked.getResourceLogHashMap().get(timestamp);
    }

    public boolean isResourceSaved(String timestamp)
    {
        return resourcesSaved.getResourceLogHashMap().containsKey(timestamp);
    }

    public ResourceLog getResourceSaved(String timestamp)
    {
        return resourcesSaved.getResourceLogHashMap().get(timestamp);
    }

    public boolean isResourceClickedAndSaved(String timestamp)
    {
        return resClickAndSaved.getResourceLogHashMap().containsKey(timestamp);
    }

    public ResourceLog getResourceClickedAndSaved(String timestamp)
    {
        return resClickAndSaved.getResourceLogHashMap().get(timestamp);
    }
}
