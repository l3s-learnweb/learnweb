package de.l3s.searchlogclient;

import java.io.Serializable;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;

import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;

import org.apache.log4j.Logger;

import com.sun.jersey.api.client.ClientHandlerException;

import de.l3s.learnweb.Learnweb;
import de.l3s.learnweb.beans.ApplicationBean;
import de.l3s.searchlogclient.jaxb.CommentonSearch;
import de.l3s.searchlogclient.jaxb.HistoryByDate;
import de.l3s.searchlogclient.jaxb.QueryHistory;
import de.l3s.searchlogclient.jaxb.QueryLog;
import de.l3s.searchlogclient.jaxb.ResourceLog;
import de.l3s.searchlogclient.jaxb.Tag;
import de.l3s.searchlogclient.jaxb.ViewingTime;

@ManagedBean
@SessionScoped
public class SearchHistoryBean extends ApplicationBean implements Serializable
{

    private static final long serialVersionUID = -1722483813729380294L;
    private static final Logger log = Logger.getLogger(SearchHistoryBean.class);

    private String newSearchComment;
    private Tag selectedTag;
    private String tagName;
    private ArrayList<HistoryByDate> searchHistoryLogs;
    private ArrayList<HistoryByDate> queryHistory;
    private LinkedList<ResourceLog> clickedResourcesList;
    private LinkedList<ResourceLog> savedResourcesList;
    private String selectedQueryTimestamp;
    private String fromFilterQueries;
    private String toFilterQueries;
    boolean displaySimilarQueries;
    boolean displaySelectedQuery;
    private String resultsetTime;
    private String url;
    private int selectedTab;

    private transient SearchLogClient searchLogClient;

    public SearchHistoryBean()
    {
        queryHistory = new ArrayList<HistoryByDate>();
        searchHistoryLogs = new ArrayList<HistoryByDate>();
        clickedResourcesList = new LinkedList<ResourceLog>();
        savedResourcesList = new LinkedList<ResourceLog>();
        displaySelectedQuery = false;
        displaySimilarQueries = false;
        newSearchComment = "";
        tagName = "";
        selectedTab = 0;
    }

    private SearchLogClient getSearchLogClient()
    {
        if(null == searchLogClient)
            searchLogClient = Learnweb.getInstance().getSearchlogClient();
        return searchLogClient;
    }

    /**
     * To display the search history data in the right panel
     */
    public void updateSearchHistory()
    {
        try
        {
            int userId = getUser() == null ? -1 : getUser().getId();
            if(userId > 0)
            {
                //searchHistoryLogs = getSearchLogClient().getSearchHistoryByDate(userId);
                //convertResourceLogHashMaptoList();
                //createResourceLogTimeline();
                displaySelectedQuery = false;

                QueryLog currentQuery = getSearchLogClient().getRecentQuery(userId);
                if(currentQuery.getQuery() != null)
                {
                    queryHistory = getSearchLogClient().getQueryHistory(userId, currentQuery.getQuery());

                    if(queryHistory.size() > 0)
                        displaySimilarQueries = true;
                    else
                        displaySimilarQueries = false;

                    for(HistoryByDate historyByDate : queryHistory)
                    {
                        for(QueryHistory queryHistory : historyByDate.getQueryHistory())
                        {
                            if(queryHistory.getQueryTimestamp().equals(currentQuery.getTimestamp()))
                            {
                                queryHistory.setQueryTime("now");
                            }

                            if(queryHistory.getQueryTimestamp().equals(selectedQueryTimestamp))
                            {
                                queryHistory.setQueryselected(true);
                                displaySelectedQuery = true;
                                resultsetTime = queryHistory.getQueryDate() + " " + queryHistory.getQueryTime();
                            }
                        }
                    }
                }
            }
        }
        catch(ClientHandlerException e)
        {
            addMessage(FacesMessage.SEVERITY_INFO, "Search Tracker service is down");
        }
        catch(RuntimeException e)
        {
            addMessage(FacesMessage.SEVERITY_INFO, e.getMessage());
        }
    }

    /**
     * To redirect to the explore history page on clicking the list view button under search history tools
     * 
     * @return
     */
    public String getExplorePage()
    {
        return getTemplateDir() + "/explore.xhtml?faces-redirect=true";
    }

    /**
     * To filter the search history by time
     */
    public void filterQueries()
    {
        try
        {
            searchHistoryLogs.clear();
            searchHistoryLogs = getSearchLogClient().filterSearchHistoryByDates(getUser().getId(), fromFilterQueries, toFilterQueries);
            convertResourceLogHashMaptoList();
        }
        catch(ClientHandlerException e)
        {
            addMessage(FacesMessage.SEVERITY_INFO, "Search Tracker service is down");
        }
        catch(RuntimeException e)
        {
            addMessage(FacesMessage.SEVERITY_INFO, e.getMessage());
        }
    }

    /**
     * To retrieve the list of resources that were clicked during the current search process.
     * 
     * @return
     */
    public LinkedList<ResourceLog> getClickedResourcesList()
    {
        try
        {
            clickedResourcesList = getSearchLogClient().getResourceClickList();

            for(ResourceLog clickedResource : clickedResourcesList)
            {
                for(ViewingTime viewTime : getSearchLogClient().getViewingTimeList())
                {
                    if(clickedResource.getResultsetId() == viewTime.getResultsetId() && clickedResource.getResourceRank() == viewTime.getResourceRank())
                    {
                        clickedResource.setViewTime((viewTime.getEndTime().getTime() - viewTime.getStartTime().getTime()) / 1000 % 60);
                    }
                }
            }
            return clickedResourcesList;
        }
        catch(ClientHandlerException e)
        {
            addMessage(FacesMessage.SEVERITY_INFO, "Search Tracker service is down");
            return null;
        }
        catch(RuntimeException e)
        {
            addMessage(FacesMessage.SEVERITY_INFO, e.getMessage());
            return null;
        }
    }

    /**
     * To retrieve the list of resources that were saved during the current search process.
     * 
     * @return
     */
    public LinkedList<ResourceLog> getSavedResourcesList()
    {
        try
        {
            savedResourcesList = getSearchLogClient().getResourceSavedList();

            for(ResourceLog savedResource : savedResourcesList)
            {
                for(ViewingTime viewTime : getSearchLogClient().getViewingTimeList())
                {
                    if(savedResource.getResultsetId() == viewTime.getResultsetId() && savedResource.getResourceRank() == viewTime.getResourceRank())
                    {
                        savedResource.setViewTime((viewTime.getEndTime().getTime() - viewTime.getStartTime().getTime()) / 1000 % 60);
                    }
                }
            }
            return savedResourcesList;
        }
        catch(ClientHandlerException e)
        {
            addMessage(FacesMessage.SEVERITY_INFO, "Search Tracker service is down");
            return null;
        }
        catch(RuntimeException e)
        {
            addMessage(FacesMessage.SEVERITY_INFO, e.getMessage());
            return null;
        }
    }

    /**
     * For adding a comment to a particular search.
     */
    public void addSearchComment()
    {

        Date searchCommentTime = new Date();

        SimpleDateFormat dateToTimestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        SimpleDateFormat dateToTime = new SimpleDateFormat("HH:mm:ss");

        int userId = getUser() == null ? -1 : getUser().getId(); // search can be anonymous
        if(userId > 0)
        {
            try
            {
                getSearchLogClient().passSearchComment(newSearchComment, userId, dateToTimestamp.format(searchCommentTime), getUser().getUsername(), dateToTime.format(searchCommentTime));
                newSearchComment = "";
            }
            catch(ClientHandlerException e)
            {
                addMessage(FacesMessage.SEVERITY_INFO, "Search Tracker service is down");
            }
            catch(RuntimeException e)
            {
                addMessage(FacesMessage.SEVERITY_INFO, e.getMessage());
            }
        }

    }

    /**
     * Adding tags to the search
     */
    public void addTag()
    {
        int userId = getUser() == null ? -1 : getUser().getId(); // search can be anonymous
        if(userId > 0)
        {
            try
            {
                getSearchLogClient().addToTagList(tagName, userId, "tagNamesList");
            }
            catch(Exception e)
            {
                addMessage(FacesMessage.SEVERITY_INFO, "Search Tracker service is down");
            }
        }
    }

    /**
     * Deleting the added tags by clicking on the close button beside the tag
     */
    public void onDeleteTag()
    {
        try
        {
            getSearchLogClient().removeFromTagList(selectedTag, "tagNamesList");
        }
        catch(Exception e)
        {
            addMessage(FacesMessage.SEVERITY_INFO, "Search Tracker service is down");
        }
    }

    /**
     * To convert the action on resource hashmap to a list for the given search history
     */
    public void convertResourceLogHashMaptoList()
    {

        for(HistoryByDate historyByDate : searchHistoryLogs)
        {

            for(QueryHistory queryHistory : historyByDate.getQueryHistory())
            {

                Iterator<String> iter = queryHistory.getResourcesSaved().getResourceLogHashMap().keySet().iterator();
                while(iter.hasNext())
                {
                    String key = iter.next();
                    ResourceLog resourceClicked = queryHistory.getResourcesclicked().getResourceLogHashMap().get(key);

                    if(resourceClicked != null)
                    {
                        queryHistory.getResClickAndSavedList().getResourceLog().add(resourceClicked);
                        queryHistory.getResourcesclicked().getResourceLogHashMap().remove(key);
                    }
                    else
                    {
                        ResourceLog resourceSaved = queryHistory.getResourcesSaved().getResourceLogHashMap().get(key);
                        queryHistory.getResourceSavedList().getResourceLog().add(resourceSaved);
                    }
                }

                iter = queryHistory.getResourcesclicked().getResourceLogHashMap().keySet().iterator();
                while(iter.hasNext())
                {
                    String key = iter.next();
                    ResourceLog resourceClicked = queryHistory.getResourcesclicked().getResourceLogHashMap().get(key);
                    queryHistory.getResourceClickList().getResourceLog().add(resourceClicked);
                }
            }
        }

    }

    /**
     * To create a timeline to display the resource action events in reverse chronological order
     */
    public void createResourceLogTimeline()
    {
        ArrayList<String> datesList = new ArrayList<String>();
        ArrayList<String> timestampList = new ArrayList<String>();
        SimpleDateFormat historyDate = new SimpleDateFormat("MMM dd, yyyy");
        SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date queryDate = null;
        String history_queryDate = "";
        String resourceLogTimestamp;

        for(HistoryByDate historyByDate : searchHistoryLogs)
        {

            for(QueryHistory queryHistory : historyByDate.getQueryHistory())
            {
                try
                {
                    queryHistory.getResourcesclicked().getResourceLogHashMap().clear();
                    queryHistory.getResourcesSaved().getResourceLogHashMap().clear();

                    for(ResourceLog clickedResource : queryHistory.getResourceClickList().getResourceLog())
                    {
                        resourceLogTimestamp = clickedResource.getTimestamp();
                        queryDate = dateFormatter.parse(resourceLogTimestamp);
                        history_queryDate = historyDate.format(queryDate);

                        timestampList.add(resourceLogTimestamp);

                        if(!datesList.contains(history_queryDate))
                        {
                            datesList.add(history_queryDate);
                        }

                        queryHistory.getResourcesclicked().getResourceLogHashMap().put(resourceLogTimestamp, clickedResource);
                    }

                    for(ResourceLog savedResource : queryHistory.getResourceSavedList().getResourceLog())
                    {
                        resourceLogTimestamp = savedResource.getTimestamp();
                        queryDate = dateFormatter.parse(resourceLogTimestamp);
                        history_queryDate = historyDate.format(queryDate);
                        timestampList.add(resourceLogTimestamp);

                        if(!datesList.contains(history_queryDate))
                        {
                            datesList.add(history_queryDate);
                        }

                        queryHistory.getResourcesSaved().getResourceLogHashMap().put(resourceLogTimestamp, savedResource);
                    }

                    for(ResourceLog clickAndSavedResource : queryHistory.getResClickAndSavedList().getResourceLog())
                    {
                        resourceLogTimestamp = clickAndSavedResource.getTimestamp();
                        queryDate = dateFormatter.parse(resourceLogTimestamp);
                        history_queryDate = historyDate.format(queryDate);
                        timestampList.add(resourceLogTimestamp);

                        if(!datesList.contains(history_queryDate))
                        {
                            datesList.add(history_queryDate);
                        }

                        queryHistory.getResClickAndSaved().getResourceLogHashMap().put(resourceLogTimestamp, clickAndSavedResource);
                    }

                    Collections.sort(datesList);
                    Collections.reverse(datesList);
                    Collections.sort(timestampList);

                    for(String date : datesList)
                    {
                        Timeline timelinedate = new Timeline();
                        timelinedate.setDate(date);

                        for(String timestamp : timestampList)
                        {
                            queryDate = dateFormatter.parse(timestamp);
                            history_queryDate = historyDate.format(queryDate);
                            if(history_queryDate.equals(date))
                            {
                                timelinedate.getTimestamps().add(timestamp);
                            }
                        }
                        queryHistory.getTimelines().add(timelinedate);
                    }

                }
                catch(ParseException e)
                {
                    log.error("unhandled error", e);
                }

            }
        }
    }

    /**
     * To return the list of comments currently entered by the user
     * 
     * @return
     */
    public LinkedList<CommentonSearch> getSearchComments()
    {
        try
        {
            return getSearchLogClient().getSearchCommentsList();
        }
        catch(ClientHandlerException e)
        {
            addMessage(FacesMessage.SEVERITY_INFO, "Search Tracker service is down");
            return null;
        }
        catch(RuntimeException e)
        {
            addMessage(FacesMessage.SEVERITY_INFO, e.getMessage());
            return null;
        }
    }

    /**
     * To display the list of tags entered by the user
     * 
     * @return
     */
    public ArrayList<Tag> getTags()
    {
        try
        {
            return getSearchLogClient().getTagNamesList();
        }
        catch(ClientHandlerException e)
        {
            addMessage(FacesMessage.SEVERITY_INFO, "Search Tracker service is down");
            return null;
        }
        catch(RuntimeException e)
        {
            addMessage(FacesMessage.SEVERITY_INFO, e.getMessage());
            return null;
        }
    }

    public ArrayList<String> completetags(String query)
    {
        try
        {
            int userId = (getUser() == null ? -1 : getUser().getId());
            ArrayList<Tag> tags = getSearchLogClient().getTagsByUserId(userId);
            ArrayList<String> tagNames = new ArrayList<String>();
            for(Tag tag : tags)
            {
                String tagName = tag.getName();
                if(tagName.toLowerCase().startsWith(query))
                {
                    tagNames.add(tagName);
                }
            }
            return tagNames;
        }
        catch(ClientHandlerException e)
        {
            addMessage(FacesMessage.SEVERITY_INFO, "Search Tracker service is down");
            return null;
        }
        catch(RuntimeException e)
        {
            addMessage(FacesMessage.SEVERITY_INFO, e.getMessage());
            return null;
        }
    }

    public ArrayList<HistoryByDate> getSearchHistoryLogs()
    {
        return searchHistoryLogs;
    }

    public ArrayList<HistoryByDate> getQueryHistory()
    {
        return queryHistory;
    }

    public String getNewSearchComment()
    {
        return newSearchComment;
    }

    public void setNewSearchComment(String newSearchComment)
    {
        this.newSearchComment = newSearchComment;
    }

    public String getFromFilterQueries()
    {
        return fromFilterQueries;
    }

    public void setFromFilterQueries(String fromFilterQueries)
    {
        this.fromFilterQueries = fromFilterQueries;
    }

    public String getToFilterQueries()
    {
        return toFilterQueries;
    }

    public void setToFilterQueries(String toFilterQueries)
    {
        this.toFilterQueries = toFilterQueries;
    }

    public String getSelectedQueryTimestamp()
    {
        return selectedQueryTimestamp;
    }

    public void setSelectedQueryTimestamp(String selectedQueryTimestamp)
    {
        this.selectedQueryTimestamp = selectedQueryTimestamp;
    }

    public boolean isDisplaySimilarQueries()
    {
        return displaySimilarQueries;
    }

    public void setDisplaySimilarQueries(boolean displaySimilarQueries)
    {
        this.displaySimilarQueries = displaySimilarQueries;
    }

    public boolean isDisplaySelectedQuery()
    {
        return displaySelectedQuery;
    }

    public void setDisplaySelectedQuery(boolean displaySelectedQuery)
    {
        this.displaySelectedQuery = displaySelectedQuery;
    }

    public String getResultsetTime()
    {
        return resultsetTime;
    }

    public void setResultsetTime(String resultsetTime)
    {
        this.resultsetTime = resultsetTime;
    }

    public String getUrl()
    {
        return url;
    }

    public void setUrl(String url)
    {
        this.url = url;
    }

    public String getTagName()
    {
        String tempTagName = tagName;
        tagName = "";
        return tempTagName;
    }

    public void setTagName(String tagName)
    {
        this.tagName = tagName;
    }

    public Tag getSelectedTag()
    {
        return selectedTag;
    }

    public void setSelectedTag(Tag selectedTag)
    {
        this.selectedTag = selectedTag;
    }

    public int getSelectedTab()
    {
        return selectedTab;
    }

    public void setSelectedTab(int selectedTab)
    {
        this.selectedTab = selectedTab;
    }
}
