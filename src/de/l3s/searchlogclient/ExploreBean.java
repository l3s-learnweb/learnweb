package de.l3s.searchlogclient;

import java.io.Serializable;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;

import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;
import javax.faces.context.FacesContext;
import javax.servlet.http.HttpServletResponse;

import com.sun.jersey.api.client.ClientHandlerException;

import de.l3s.learnweb.Learnweb;
import de.l3s.learnwebBeans.ApplicationBean;
import de.l3s.searchlogclient.jaxb.HistoryByDate;
import de.l3s.searchlogclient.jaxb.QueryHistory;
import de.l3s.searchlogclient.jaxb.ResourceLog;

@ManagedBean
@SessionScoped
public class ExploreBean extends ApplicationBean implements Serializable
{
    private static final long serialVersionUID = 6889417588459447502L;

    public enum MODE
    {
        task_based,
        list_view,
        resultset_view
    };

    private int page;
    private int pageSize;
    private SearchLogClient searchLogClient;
    private MODE exploreMode;
    ArrayList<HistoryByDate> searchHistoryLogs;
    private String fromFilterQueries;
    private String toFilterQueries;
    private boolean selectedQuery;
    private HashSet<Integer> toBeDeletedIDSet;

    public ExploreBean()
    {
        searchLogClient = Learnweb.getInstance().getSearchlogClient();
        exploreMode = MODE.list_view;
        fromFilterQueries = null;
        toFilterQueries = null;
        toBeDeletedIDSet = new HashSet<Integer>();
        //page = 0;
        pageSize = 20;
    }

    public void preRenderView()
    {

        if(!FacesContext.getCurrentInstance().isPostback())
        {
            toBeDeletedIDSet.clear();
            selectedQuery = false;
            searchHistoryLogs = null;
            try
            {
                int userId = getUser() == null ? -1 : getUser().getId();
                searchLogClient.getRecentQuery(userId);
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

        // stop caching (back button problem)
        HttpServletResponse response = (HttpServletResponse) getFacesContext().getExternalContext().getResponse();

        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate"); // HTTP 1.1.
        response.setHeader("Pragma", "no-cache"); // HTTP 1.0.
        response.setDateHeader("Expires", 0); // Proxies.
    }

    /**
     * To redirect to the next page of the search history
     * 
     * @param buttonType
     * @return
     */
    public String redirect(String buttonType)
    {
        int pageNo = page;// + 1;
        return getTemplateDir() + "/explore.xhtml?page=" + pageNo + "&amp;mode=list_view&amp;faces-redirect=true";
    }

    public void clearFilters()
    {
        fromFilterQueries = null;
        toFilterQueries = null;
    }

    /**
     * To filter the search history by time
     */
    public void filterQueries()
    {
        searchHistoryLogs.clear();
        try
        {
            searchHistoryLogs = searchLogClient.filterSearchHistoryByDates(getUser().getId(), fromFilterQueries, toFilterQueries);
            convertResourceLogHashMaptoList();
            createResourceLogTimeline();
        }
        catch(ClientHandlerException e)
        {
            addGrowl(FacesMessage.SEVERITY_INFO, "searchTrackerDown");
        }
        catch(RuntimeException e)
        {
            addGrowl(FacesMessage.SEVERITY_INFO, e.getMessage());
        }
    }

    /**
     * To delete the queries that were selected by the user from the search history.
     */
    public void deleteSelectedQueries()
    {
        try
        {
            Iterator<Integer> iter = toBeDeletedIDSet.iterator();
            ArrayList<Integer> resultsetIdList = new ArrayList<Integer>();

            while(iter.hasNext())
            {
                Integer resultsetId = iter.next();
                resultsetIdList.add(resultsetId);
            }
            toBeDeletedIDSet.clear();
            searchHistoryLogs = null;
            page--;
            searchLogClient.deleteUserQueries(resultsetIdList);
        }
        catch(ClientHandlerException e)
        {
            addGrowl(FacesMessage.SEVERITY_INFO, "searchTrackerDown");
        }
        catch(RuntimeException e)
        {
            addGrowl(FacesMessage.SEVERITY_INFO, e.getMessage());
        }
    }

    /**
     * To return the entire search history corresponding to a user
     * 
     * @return
     */
    public ArrayList<HistoryByDate> getSearchHistoryLogs()
    {
        try
        {
            if(searchHistoryLogs == null)
            {
                int userId = getUser() == null ? -1 : getUser().getId();
                searchHistoryLogs = searchLogClient.getSearchHistoryByPages(userId, page * pageSize, pageSize);
                //page++;
                convertResourceLogHashMaptoList();
                createResourceLogTimeline();
            }
            if(searchHistoryLogs.size() == 0 && FacesContext.getCurrentInstance().getMessageList().size() == 0)
                FacesContext.getCurrentInstance().addMessage("no_explore_history", new FacesMessage(FacesMessage.SEVERITY_INFO, "Info", "There are no search history entries to be displayed"));
            return searchHistoryLogs;
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

    public void setSearchHistoryLogs(ArrayList<HistoryByDate> searchHistoryLogs)
    {
        this.searchHistoryLogs = searchHistoryLogs;
    }

    /**
     * To convert the hashmap of action on resource events to a list given the search history
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
     * To create a timeline of the various action on resource events displayed in reverse choronological order
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
                    datesList.clear();
                    timestampList.clear();
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
                    e.printStackTrace();
                }

            }
        }
    }

    public String getExploreMode()
    {
        return exploreMode.name();
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

    public void setExploreMode(String exploreMode)
    {
        if(exploreMode.equals("task_based"))
            this.exploreMode = MODE.task_based;
        else if(exploreMode.equals("list_view"))
            this.exploreMode = MODE.list_view;
        else if(exploreMode.equals("resultset_view"))
            this.exploreMode = MODE.resultset_view;
    }

    public int getPage()
    {
        return page;
    }

    public void setPage(int page)
    {
        this.page = page;
    }

    public int getPageSize()
    {
        return pageSize;
    }

    public void setPageSize(int pageSize)
    {
        this.pageSize = pageSize;
    }

    public void pushToDeleteQueue(Integer id)
    {
        if(toBeDeletedIDSet.contains(id))
        {
            toBeDeletedIDSet.remove(id);
        }
        else
        {
            toBeDeletedIDSet.add(id);
        }
    }

    public boolean isSelectedQuery()
    {
        return selectedQuery;
    }

    public void setSelectedQuery(boolean selectedQuery)
    {
        this.selectedQuery = selectedQuery;
    }

}
